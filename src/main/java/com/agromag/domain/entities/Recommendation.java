package com.agromag.domain.entities;

import com.agromag.domain.enums.RecommendationSource;
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

// Recomendación generada por IA o por reglas para un cultivo específico
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

	// null = el productor aún no tomó una decisión sobre esta recomendación
	private Boolean followed;

	@Enumerated(EnumType.STRING)
	@Column
	private RecommendationSource source;

	private BigDecimal temperature;

	private BigDecimal humidity;

	// Timestamp explícito — controlado por RecommendationService, no por Hibernate
	@Column(name = "generated_at", nullable = false)
	private LocalDateTime generatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status")
	private SyncStatus syncStatus = SyncStatus.PENDING;

	@Version
	@Column(name = "version")
	private Long version;
}
