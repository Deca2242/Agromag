package com.agromag.repository;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CropRepository extends JpaRepository<Crop, UUID> {

	List<Crop> findByProfile_Id(UUID profileId);

	List<Crop> findByMunicipality(String municipality);

	List<Crop> findBySyncStatus(SyncStatus syncStatus);
}
