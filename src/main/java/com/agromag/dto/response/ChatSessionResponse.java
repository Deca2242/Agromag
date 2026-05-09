package com.agromag.dto.response;

import com.agromag.domain.entities.ChatSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full response DTO for a chat session including all messages.
 *
 * @param id the session identifier
 * @param title the session title
 * @param createdAt session creation timestamp
 * @param updatedAt last activity timestamp
 * @param messages ordered list of messages in the session
 */
public record ChatSessionResponse(
		UUID id,
		String title,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<ChatMessageResponse> messages
) {
	/** Factory method from JPA entity. */
	public static ChatSessionResponse from(ChatSession session) {
		List<ChatMessageResponse> msgs = session.getMessages().stream()
				.map(ChatMessageResponse::from)
				.toList();
		return new ChatSessionResponse(
				session.getId(),
				session.getTitle(),
				session.getCreatedAt(),
				session.getUpdatedAt(),
				msgs
		);
	}
}
