package com.agromag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for sending a message in a chat session.
 *
 * @param message the user's message content
 */
public record ChatMessageRequest(
		@NotBlank @Size(max = 2000) String message
) {}
