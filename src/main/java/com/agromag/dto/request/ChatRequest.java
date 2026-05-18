package com.agromag.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
		@NotBlank @Size(max = 2000) String message,
		@Size(max = 10)
		@Valid List<ChatTurn> history
) {
	public record ChatTurn(
			@NotBlank @Pattern(regexp = "user|assistant") String role,
			@NotBlank @Size(max = 2000) String content
	) {}
}
