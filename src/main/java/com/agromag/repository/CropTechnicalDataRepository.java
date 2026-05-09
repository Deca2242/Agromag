package com.agromag.repository;

import com.agromag.domain.entities.CropTechnicalData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link CropTechnicalData}.
 */
public interface CropTechnicalDataRepository extends JpaRepository<CropTechnicalData, Long> {

	Optional<CropTechnicalData> findByCropId(UUID cropId);

	boolean existsByCropId(UUID cropId);
}
