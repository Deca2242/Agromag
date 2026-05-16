package com.agromag.repository;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// Acceso a datos de cultivos
public interface CropRepository extends JpaRepository<Crop, UUID> {

	List<Crop> findByProfile_Id(UUID profileId);

	List<Crop> findByMunicipality(Municipality municipality);

	List<Crop> findBySyncStatus(SyncStatus syncStatus);

	@Modifying
	@Query("DELETE FROM Crop c WHERE c.id = :cropId AND c.profile.id = :profileId")
	int deleteByIdAndProfileId(@Param("cropId") UUID cropId,
							  @Param("profileId") UUID profileId);
}
