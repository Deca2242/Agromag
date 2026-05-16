package com.agromag.repository;

import com.agromag.domain.entities.Alert;
import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.RecommendationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// Acceso a datos de alertas
public interface AlertRepository extends JpaRepository<Alert, UUID> {

	@EntityGraph(attributePaths = {"crop"})
	Page<Alert> findByProfile_IdOrderByCreatedAtDesc(UUID profileId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Page<Alert> findByProfile_IdAndTypeOrderByCreatedAtDesc(UUID profileId, RecommendationType type, Pageable pageable);

	List<Alert> findByProfile_IdAndReadAtIsNullOrderByCreatedAtDesc(UUID profileId);

	@Query("SELECT COUNT(a) FROM Alert a WHERE a.profile.id = :profileId AND a.readAt IS NULL")
	long countUnreadByProfileId(@Param("profileId") UUID profileId);

	@Query("SELECT COUNT(a) FROM Alert a WHERE a.profile.id = :profileId AND a.severity = :severity AND a.readAt IS NULL")
	long countUnreadByProfileIdAndSeverity(@Param("profileId") UUID profileId, @Param("severity") AlertSeverity severity);

	@Query("SELECT COUNT(a) FROM Alert a WHERE a.profile.id = :profileId AND a.readAt IS NULL AND a.severity IN (:severities)")
	long countUnreadByProfileIdAndSeverities(@Param("profileId") UUID profileId, @Param("severities") List<AlertSeverity> severities);

	int deleteAllByProfile_IdAndReadAtIsNotNull(UUID profileId);

	@Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.profile.id = :profileId AND a.type = :type " +
			"AND a.crop.id = :cropId AND a.readAt IS NULL AND a.createdAt > :since")
	boolean existsRecentUnread(@Param("profileId") UUID profileId,
							   @Param("type") com.agromag.domain.enums.RecommendationType type,
							   @Param("cropId") UUID cropId,
							   @Param("since") java.time.LocalDateTime since);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query("UPDATE Alert a SET a.readAt = :now WHERE a.id = :alertId AND a.profile.id = :profileId")
	int markAsReadDirect(@Param("alertId") UUID alertId,
						 @Param("profileId") UUID profileId,
						 @Param("now") java.time.LocalDateTime now);
}
