package com.agromag.dto.response;

import com.agromag.domain.entities.ChatMessage;
import com.agromag.domain.enums.MessageRole;

import java.time.LocalDateTime;

/**
 * Response DTO for a single chat message.
 *
 * @param id the message identifier
 * @param role the sender role (USER or ASSISTANT)
 * @param content the message text
 * @param sentAt the timestamp when the message was sent
 */
public record ChatMessageResponse(
		Long id,
		MessageRole role,
		String content,
		LocalDateTime sentAt
) {
	/** Factory method from JPA entity. */
	public static ChatMessageResponse from(ChatMessage msg) {
		return new ChatMessageResponse(msg.getId(), msg.getRole(), msg.getContent(), msg.getSentAt());
	}
}
