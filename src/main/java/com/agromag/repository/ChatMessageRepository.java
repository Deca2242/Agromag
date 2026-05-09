package com.agromag.repository;

import com.agromag.domain.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link ChatMessage}.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findBySessionIdOrderBySentAtAsc(UUID sessionId);
}
