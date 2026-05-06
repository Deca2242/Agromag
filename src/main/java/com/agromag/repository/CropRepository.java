package com.agromag.repository;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Acceso a datos de cultivos
public interface CropRepository extends JpaRepository<Crop, UUID> {

	List<Crop> findByProfile_Id(UUID profileId);

	List<Crop> findByMunicipality(Municipality municipality);

	List<Crop> findBySyncStatus(SyncStatus syncStatus);
}
