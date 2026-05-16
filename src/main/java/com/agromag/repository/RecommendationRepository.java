package com.agromag.repository;

import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de recomendaciones
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdAndFollowedIsNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdAndFollowedIsNotNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Optional<Recommendation> findTopByCrop_IdAndTypeOrderByGeneratedAtDesc(UUID cropId, RecommendationType type);

	/** Solo pendientes de decisión; las ya decididas se conservan para historial. */
	@Modifying
	@Query("DELETE FROM Recommendation r WHERE r.crop.id = :cropId AND r.type = :type AND r.followed IS NULL")
	int deleteByCrop_IdAndTypeAndFollowedIsNull(@Param("cropId") UUID cropId, @Param("type") RecommendationType type);

	@Modifying
	@Query("DELETE FROM Recommendation r WHERE r.crop.id = :cropId")
	int deleteByCrop_Id(@Param("cropId") UUID cropId);

	@EntityGraph(attributePaths = {"crop"})
	@Query("SELECT r FROM Recommendation r WHERE r.crop.profile.id = :profileId AND r.followed IS NULL ORDER BY r.generatedAt DESC")
    List<Recommendation> findPendingByProfileId(@Param("profileId") UUID profileId, org.springframework.data.domain.Pageable pageable);
}
