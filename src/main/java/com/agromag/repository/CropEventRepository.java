package com.agromag.repository;

import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.SyncStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// Acceso a datos de eventos de cultivos
public interface CropEventRepository extends JpaRepository<CropEvent, UUID> {

	@EntityGraph(attributePaths = {"crop"})
	List<CropEvent> findByCrop_IdOrderByOccurredAtDesc(UUID cropId);

	List<CropEvent> findBySyncStatus(SyncStatus syncStatus);

	@Modifying
	@Query("DELETE FROM CropEvent ce WHERE ce.crop.id = :cropId")
	int deleteByCrop_Id(@Param("cropId") UUID cropId);

	@Modifying
	@Query("""
			DELETE FROM CropEvent ce
			WHERE ce.id = :eventId
			AND ce.crop.id IN (SELECT c.id FROM Crop c WHERE c.profile.id = :profileId)
			""")
	int deleteByIdAndProfileId(@Param("eventId") UUID eventId,
							  @Param("profileId") UUID profileId);

	@EntityGraph(attributePaths = {"crop"})
	@Query("SELECT ce FROM CropEvent ce WHERE ce.crop.profile.id = :profileId ORDER BY ce.occurredAt DESC")
	List<CropEvent> findRecentByProfileId(@Param("profileId") UUID profileId, Pageable pageable);
}
