package com.agromag.controller;

import com.agromag.dto.request.ChatMessageRequest;
import com.agromag.dto.response.ChatMessageResponse;
import com.agromag.dto.response.ChatSessionResponse;
import com.agromag.dto.response.ChatSessionSummaryResponse;
import com.agromag.service.ChatService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the AI chat feature. All endpoints require JWT authentication.
 * The chat is global per user — the AI receives context from all of the user's crops.
 *
 * @since 1.1
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	/**
	 * Creates a new empty chat session.
	 *
	 * @param principal the authenticated user
	 * @return the created session summary
	 */
	@PostMapping("/sessions")
	public ResponseEntity<ChatSessionSummaryResponse> createSession(Principal principal) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		ChatSessionSummaryResponse response = chatService.createSession(profileId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Lists all chat sessions for the authenticated user.
	 *
	 * @param principal the authenticated user
	 * @return list of session summaries ordered by most recent activity
	 */
	@GetMapping("/sessions")
	public ResponseEntity<List<ChatSessionSummaryResponse>> listSessions(Principal principal) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(chatService.listSessions(profileId));
	}

	/**
	 * Returns a chat session with all its messages.
	 *
	 * @param principal the authenticated user
	 * @param sessionId the session identifier
	 * @return the full session with ordered messages
	 */
	@GetMapping("/sessions/{sessionId}")
	public ResponseEntity<ChatSessionResponse> getSession(
			Principal principal,
			@PathVariable UUID sessionId) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(chatService.getSession(sessionId, profileId));
	}

	/**
	 * Sends a message to the AI and returns the response.
	 *
	 * @param principal the authenticated user
	 * @param sessionId the session identifier
	 * @param request the user's message
	 * @return the AI assistant's response
	 */
	@PostMapping("/sessions/{sessionId}/messages")
	public ResponseEntity<ChatMessageResponse> sendMessage(
			Principal principal,
			@PathVariable UUID sessionId,
			@Valid @RequestBody ChatMessageRequest request) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		ChatMessageResponse response = chatService.sendMessage(sessionId, profileId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Deletes a chat session and all its messages.
	 *
	 * @param principal the authenticated user
	 * @param sessionId the session identifier
	 * @return 204 No Content on success
	 */
	@DeleteMapping("/sessions/{sessionId}")
	public ResponseEntity<Void> deleteSession(
			Principal principal,
			@PathVariable UUID sessionId) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		chatService.deleteSession(sessionId, profileId);
		return ResponseEntity.noContent().build();
	}
}
