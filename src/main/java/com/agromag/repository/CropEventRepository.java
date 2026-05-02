package com.agromag.repository;

import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CropEventRepository extends JpaRepository<CropEvent, UUID> {

	List<CropEvent> findByCrop_Id(UUID cropId);

	List<CropEvent> findByCrop_IdOrderByOccurredAtDesc(UUID cropId);

	List<CropEvent> findBySyncStatus(SyncStatus syncStatus);
}
