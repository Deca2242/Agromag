package com.agromag.domain.entities;

import com.agromag.domain.enums.CropType;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// Cultivo registrado por un productor
@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
public class Crop {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private Profile profile;

	@Enumerated(EnumType.STRING)
	@Column(name = "crop_type", nullable = false)
	private CropType cropType;

	@Column(name = "area_hectares")
	private BigDecimal areaHectares;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Municipality municipality;

	@Column(name = "sown_date", nullable = false)
	private LocalDate sownDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status")
	private SyncStatus syncStatus = SyncStatus.PENDING;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "crop", cascade = CascadeType.ALL)
	private List<CropEvent> events = new ArrayList<>();

	@OneToMany(mappedBy = "crop", cascade = CascadeType.ALL)
	private List<Recommendation> recommendations = new ArrayList<>();

	@PrePersist
	void prePersist() {
		if (syncStatus == null) {
			syncStatus = SyncStatus.PENDING;
		}
	}


}
