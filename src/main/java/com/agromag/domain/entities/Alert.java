package com.agromag.domain.entities;

import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.RecommendationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Alerta generada automaticamente por evaluacion de condiciones climaticas y agronomicas
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "crop_id", nullable = false)
	private Crop crop;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private Profile profile;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecommendationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AlertSeverity severity;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(name = "crop_tag", nullable = false, length = 100)
	private String cropTag;

	@Column(name = "icon_code", nullable = false)
	private Integer iconCode;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Version
	@Column(name = "version")
	private Long version;
}
