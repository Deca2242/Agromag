package com.agromag.service;

import com.agromag.domain.entities.ChatMessage;
import com.agromag.domain.entities.ChatSession;
import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.MessageRole;
import com.agromag.dto.request.ChatMessageRequest;
import com.agromag.dto.response.ChatMessageResponse;
import com.agromag.dto.response.ChatSessionResponse;
import com.agromag.dto.response.ChatSessionSummaryResponse;
import com.agromag.exception.AiServiceException;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.exception.UnauthorizedCropAccessException;
import com.agromag.repository.ChatMessageRepository;
import com.agromag.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing chat sessions and exchanging messages with the AI.
 * The AI receives a system prompt containing all of the user's agronomic data
 * plus the conversation history, enabling contextual and personalized responses.
 *
 * @since 1.1
 */
@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	/** Maximum number of past messages to include in the AI context window. */
	private static final int MAX_HISTORY_MESSAGES = 40;

	private final ChatSessionRepository sessionRepository;
	private final ChatMessageRepository messageRepository;
	private final ProfileService profileService;
	private final ChatContextBuilder contextBuilder;
	private final ChatClient chatClient;

	public ChatService(ChatSessionRepository sessionRepository,
					   ChatMessageRepository messageRepository,
					   ProfileService profileService,
					   ChatContextBuilder contextBuilder,
					   ChatClient.Builder chatClientBuilder) {
		this.sessionRepository = sessionRepository;
		this.messageRepository = messageRepository;
		this.profileService = profileService;
		this.contextBuilder = contextBuilder;
		this.chatClient = chatClientBuilder.build();
	}

	/**
	 * Creates a new empty chat session for the user.
	 *
	 * @param profileId the authenticated user's profile identifier
	 * @return a summary of the created session
	 */
	@Transactional
	public ChatSessionSummaryResponse createSession(UUID profileId) {
		Profile profile = profileService.getProfileById(profileId);

		ChatSession session = new ChatSession();
		session.setId(UUID.randomUUID());
		session.setProfile(profile);
		session.setTitle("Nueva conversación");
		sessionRepository.save(session);

		log.info("create_chat_session sessionId={} profileId={}", session.getId(), profileId);
		return ChatSessionSummaryResponse.from(session);
	}

	/**
	 * Sends a user message, invokes the AI with full context, persists both
	 * messages and returns the AI's response.
	 *
	 * @param sessionId the chat session identifier
	 * @param profileId the authenticated user's profile identifier
	 * @param request the user's message
	 * @return the AI assistant's response message
	 * @throws ResourceNotFoundException if the session does not exist
	 * @throws AiServiceException if the AI call fails
	 */
	@Transactional
	public ChatMessageResponse sendMessage(UUID sessionId, UUID profileId, ChatMessageRequest request) {
		ChatSession session = findSessionAndValidateOwnership(sessionId, profileId);
		Profile profile = session.getProfile();

		// 1. Persist the user's message
		ChatMessage userMsg = new ChatMessage();
		userMsg.setSession(session);
		userMsg.setRole(MessageRole.USER);
		userMsg.setContent(request.message());
		messageRepository.save(userMsg);

		// 2. Build AI messages: system prompt + history + current message
		String systemPrompt = contextBuilder.buildSystemPrompt(profile);
		List<Message> aiMessages = buildAiMessages(systemPrompt, session, request.message());

		// 3. Call AI
		String aiResponseText;
		try {
			aiResponseText = chatClient.prompt()
					.messages(aiMessages)
					.call()
					.content();

			if (aiResponseText == null || aiResponseText.isBlank()) {
				aiResponseText = "Lo siento, no pude generar una respuesta. Intenta reformular tu pregunta.";
			}
		} catch (Exception e) {
			log.error("chat_ai_error sessionId={} profileId={}", sessionId, profileId, e);
			throw new AiServiceException("Error al comunicarse con la IA", e);
		}

		// 4. Persist assistant's response
		ChatMessage assistantMsg = new ChatMessage();
		assistantMsg.setSession(session);
		assistantMsg.setRole(MessageRole.ASSISTANT);
		assistantMsg.setContent(aiResponseText);
		messageRepository.save(assistantMsg);

		// 5. Auto-title on first message
		if (session.getMessages().size() <= 2) {
			String title = request.message().length() > 60
					? request.message().substring(0, 60) + "..."
					: request.message();
			session.setTitle(title);
			sessionRepository.save(session);
		}

		log.info("chat_message sessionId={} profileId={} userMsgLen={} aiMsgLen={}",
				sessionId, profileId, request.message().length(), aiResponseText.length());

		return ChatMessageResponse.from(assistantMsg);
	}

	/**
	 * Returns a chat session with all its messages.
	 *
	 * @param sessionId the chat session identifier
	 * @param profileId the authenticated user's profile identifier
	 * @return the full session with ordered messages
	 */
	@Transactional(readOnly = true)
	public ChatSessionResponse getSession(UUID sessionId, UUID profileId) {
		ChatSession session = findSessionAndValidateOwnership(sessionId, profileId);
		return ChatSessionResponse.from(session);
	}

	/**
	 * Lists all chat sessions for the user, ordered by most recent activity.
	 *
	 * @param profileId the authenticated user's profile identifier
	 * @return list of session summaries without message bodies
	 */
	@Transactional(readOnly = true)
	public List<ChatSessionSummaryResponse> listSessions(UUID profileId) {
		return sessionRepository.findByProfileIdOrderByUpdatedAtDesc(profileId).stream()
				.map(ChatSessionSummaryResponse::from)
				.toList();
	}

	/**
	 * Deletes a chat session and all its messages.
	 *
	 * @param sessionId the chat session identifier
	 * @param profileId the authenticated user's profile identifier
	 */
	@Transactional
	public void deleteSession(UUID sessionId, UUID profileId) {
		ChatSession session = findSessionAndValidateOwnership(sessionId, profileId);
		sessionRepository.delete(session);
		log.info("delete_chat_session sessionId={} profileId={}", sessionId, profileId);
	}

	private ChatSession findSessionAndValidateOwnership(UUID sessionId, UUID profileId) {
		ChatSession session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Sesión de chat", sessionId));

		if (!session.getProfile().getId().equals(profileId)) {
			throw new UnauthorizedCropAccessException(sessionId, profileId);
		}
		return session;
	}

	private List<Message> buildAiMessages(String systemPrompt, ChatSession session, String currentMessage) {
		List<Message> messages = new ArrayList<>();

		// System prompt with full context
		messages.add(new SystemMessage(systemPrompt));

		// Conversation history (limited to avoid token overflow)
		List<ChatMessage> history = messageRepository.findBySessionIdOrderBySentAtAsc(session.getId());
		int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
		for (int i = start; i < history.size(); i++) {
			ChatMessage msg = history.get(i);
			if (msg.getRole() == MessageRole.USER) {
				messages.add(new UserMessage(msg.getContent()));
			} else {
				messages.add(new AssistantMessage(msg.getContent()));
			}
		}

		// Current user message (already persisted but we add it to the prompt too)
		messages.add(new UserMessage(currentMessage));

		return messages;
	}
}
