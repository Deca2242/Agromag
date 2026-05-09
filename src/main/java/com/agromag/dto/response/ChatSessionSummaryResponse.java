package com.agromag.dto.response;

import com.agromag.domain.entities.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight response DTO for listing chat sessions (no messages included).
 *
 * @param id the session identifier
 * @param title the session title
 * @param createdAt session creation timestamp
 * @param updatedAt last activity timestamp
 * @param messageCount total number of messages in the session
 */
public record ChatSessionSummaryResponse(
		UUID id,
		String title,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		int messageCount
) {
	/** Factory method from JPA entity. */
	public static ChatSessionSummaryResponse from(ChatSession session) {
		return new ChatSessionSummaryResponse(
				session.getId(),
				session.getTitle(),
				session.getCreatedAt(),
				session.getUpdatedAt(),
				session.getMessages().size()
		);
	}
}
