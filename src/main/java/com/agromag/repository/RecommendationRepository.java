package com.agromag.repository;

import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationType;
<<<<<<< Updated upstream
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
=======
<<<<<<< Updated upstream
=======
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
>>>>>>> Stashed changes
>>>>>>> Stashed changes
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de recomendaciones
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

<<<<<<< Updated upstream
	List<Recommendation> findByCrop_Id(UUID cropId);

	Page<Recommendation> findByCrop_IdOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	Page<Recommendation> findByCrop_IdAndFollowedIsNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	Page<Recommendation> findByCrop_IdAndFollowedIsNotNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	List<Recommendation> findByCrop_IdAndType(UUID cropId, RecommendationType type);

=======
	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdAndFollowedIsNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
	Page<Recommendation> findByCrop_IdAndFollowedIsNotNullOrderByGeneratedAtDesc(UUID cropId, Pageable pageable);

	@EntityGraph(attributePaths = {"crop"})
>>>>>>> Stashed changes
	Optional<Recommendation> findTopByCrop_IdAndTypeOrderByGeneratedAtDesc(UUID cropId, RecommendationType type);

	/** Solo pendientes de decisión; las ya decididas se conservan para historial. */
	void deleteByCrop_IdAndTypeAndFollowedIsNull(UUID cropId, RecommendationType type);
}
