package com.agromag.domain.entities;

import com.agromag.domain.enums.CropType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "crop_parameters")
@Getter
@Setter
@NoArgsConstructor
public class CropParameter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "crop_type", nullable = false, unique = true)
	private CropType cropType;

	@Column(name = "suggested_spacing", nullable = false)
	private String suggestedSpacing;

	@Column(name = "growth_cycle_days", nullable = false)
	private Integer growthCycleDays;

	@Column(name = "optimal_temp_min", nullable = false)
	private BigDecimal optimalTempMin;

	@Column(name = "optimal_temp_max", nullable = false)
	private BigDecimal optimalTempMax;

	@Column(name = "humidity_min", nullable = false)
	private BigDecimal humidityMin;

	@Column(name = "humidity_max", nullable = false)
	private BigDecimal humidityMax;

	@Column(name = "ph_min", nullable = false)
	private BigDecimal phMin;

	@Column(name = "ph_max", nullable = false)
	private BigDecimal phMax;

	@Column(name = "ec_min", nullable = false)
	private BigDecimal ecMin;

	@Column(name = "ec_max", nullable = false)
	private BigDecimal ecMax;

	@Column(name = "irrigation_needs", nullable = false)
	private String irrigationNeeds; // ml/day or ml/week

	@Column(name = "recommended_fertilizer", nullable = false)
	private String recommendedFertilizer;

	@Column(name = "planting_depth_cm", nullable = false)
	private BigDecimal plantingDepthCm;
}
