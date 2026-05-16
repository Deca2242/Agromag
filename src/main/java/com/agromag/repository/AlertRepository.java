package com.agromag.repository;

import com.agromag.domain.entities.Alert;
import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.RecommendationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@EntityGraph(attributePaths = {"crop"})
	List<Alert> findByProfile_IdAndReadAtIsNullOrderByCreatedAtDesc(UUID profileId);

	@Query("SELECT COUNT(a) FROM Alert a WHERE a.profile.id = :profileId AND a.readAt IS NULL")
	long countUnreadByProfileId(@Param("profileId") UUID profileId);

	@Query("SELECT COUNT(a) FROM Alert a WHERE a.profile.id = :profileId AND a.severity = :severity AND a.readAt IS NULL")
	long countUnreadByProfileIdAndSeverity(@Param("profileId") UUID profileId, @Param("severity") AlertSeverity severity);

	@Query("SELECT COUNT(a) FROM Alert a JOIN a.crop c WHERE a.profile.id = :profileId AND a.readAt IS NULL AND a.severity IN (:severities)")
	long countUnreadByProfileIdAndSeverities(@Param("profileId") UUID profileId, @Param("severities") List<AlertSeverity> severities);

	@Modifying
	@Query("DELETE FROM Alert a WHERE a.profile.id = :profileId AND a.readAt IS NOT NULL")
	int deleteAllByProfile_IdAndReadAtIsNotNull(@Param("profileId") UUID profileId);

	@Modifying
	@Query("DELETE FROM Alert a WHERE a.crop.id = :cropId")
	int deleteByCrop_Id(@Param("cropId") UUID cropId);

	@Modifying
	@Query("DELETE FROM Alert a WHERE a.id = :alertId AND a.profile.id = :profileId")
	int deleteByIdAndProfileId(@Param("alertId") UUID alertId,
							   @Param("profileId") UUID profileId);

	@Modifying
	@Query("DELETE FROM Alert a WHERE a.crop.id NOT IN (SELECT c.id FROM Crop c)")
	int deleteOrphanedByMissingCrop();

	@Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.profile.id = :profileId AND a.type = :type " +
			"AND a.crop.id = :cropId AND a.readAt IS NULL AND a.createdAt > :since")
	boolean existsRecentUnread(@Param("profileId") UUID profileId,
							   @Param("type") com.agromag.domain.enums.RecommendationType type,
							   @Param("cropId") UUID cropId,
							   @Param("since") java.time.LocalDateTime since);

	@Modifying
	@Query("UPDATE Alert a SET a.readAt = :now WHERE a.id = :alertId AND a.profile.id = :profileId")
	int markAsReadDirect(@Param("alertId") UUID alertId,
						 @Param("profileId") UUID profileId,
						 @Param("now") java.time.LocalDateTime now);

	@Modifying
	@Query("UPDATE Alert a SET a.readAt = :now WHERE a.profile.id = :profileId AND a.readAt IS NULL")
	int markAllAsReadDirect(@Param("profileId") UUID profileId,
						  @Param("now") java.time.LocalDateTime now);
}
