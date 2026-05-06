package com.agromag.domain.entities;

import com.agromag.domain.enums.EventType;
import com.agromag.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Evento registrado sobre un cultivo (riego, fertilizaci\u00f3n, observaci\u00f3n, etc.)
@Entity
@Table(name = "crop_events")
@Getter
@Setter
@NoArgsConstructor
public class CropEvent {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "crop_id", nullable = false)
	private Crop crop;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false)
	private EventType eventType;

	private BigDecimal quantity;

	private String unit;

	@Column(length = 2000)
	private String notes;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status")
	private SyncStatus syncStatus = SyncStatus.PENDING;

	@PrePersist
	void prePersist() {
		if (syncStatus == null) {
			syncStatus = SyncStatus.PENDING;
		}
	}
}
