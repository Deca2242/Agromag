package com.agromag.repository;

import com.agromag.domain.entities.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link ChatSession}.
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

	List<ChatSession> findByProfileIdOrderByUpdatedAtDesc(UUID profileId);
}
