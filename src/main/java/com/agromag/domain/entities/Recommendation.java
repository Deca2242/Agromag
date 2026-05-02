package com.agromag.domain.entities;

import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "crop_id", nullable = false)
	private Crop crop;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecommendationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel level;

	@Column(nullable = false, length = 2000)
	private String message;

	private Boolean followed;

	private BigDecimal temperature;

	private BigDecimal humidity;

	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status")
	private SyncStatus syncStatus = SyncStatus.PENDING;

	@PrePersist
	void prePersist() {
		if (generatedAt == null) {
			generatedAt = LocalDateTime.now();
		}
		if (syncStatus == null) {
			syncStatus = SyncStatus.PENDING;
		}
	}
}
