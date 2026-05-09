package com.agromag.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Technical data sheet for an individual crop, containing field measurements
 * across three domains: soil/nutrition, climate/irrigation and planting techniques.
 *
 * <p>Modeled as {@code @OneToOne} with {@link Crop} — each crop has at most one
 * technical data sheet that is created/updated as a single unit.</p>
 *
 * @since 1.1
 */
@Entity
@Table(name = "crop_technical_data")
@Getter
@Setter
@NoArgsConstructor
public class CropTechnicalData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "crop_id", nullable = false, unique = true)
	private Crop crop;

	// ── a) Gestión del suelo y nutrición ──────────────────────────────────

	/** pH medido del suelo. */
	@Column(name = "soil_ph")
	private BigDecimal soilPh;

	/** Textura del suelo (ej: "Franco arcilloso"). */
	@Column(name = "soil_texture", length = 100)
	private String soilTexture;

	/** Estructura del suelo (ej: "Granular", "Blocosa"). */
	@Column(name = "soil_structure", length = 100)
	private String soilStructure;

	/** Capacidad de intercambio catiónico en meq/100g. */
	@Column(name = "cation_exchange_capacity")
	private BigDecimal cationExchangeCapacity;

	/** Nivel de nitrógeno en ppm. */
	@Column(name = "nitrogen_level")
	private BigDecimal nitrogenLevel;

	/** Nivel de fósforo en ppm. */
	@Column(name = "phosphorus_level")
	private BigDecimal phosphorusLevel;

	/** Nivel de potasio en ppm. */
	@Column(name = "potassium_level")
	private BigDecimal potassiumLevel;

	/** Índice de clorofila (medido con SPAD). */
	@Column(name = "chlorophyll_index")
	private BigDecimal chlorophyllIndex;

	/** Índice NDVI (0.0 – 1.0). */
	@Column(name = "ndvi_index")
	private BigDecimal ndviIndex;

	/** Indica si se realizó desinfección del suelo. */
	@Column(name = "soil_disinfected")
	private Boolean soilDisinfected;

	/** Notas sobre patógenos y malezas detectados en el lote. */
	@Column(name = "pathogen_notes", length = 2000)
	private String pathogenNotes;

	// ── b) Parámetros climáticos y de riego ───────────────────────────────

	/** Humedad del suelo medida en campo (%). */
	@Column(name = "soil_moisture")
	private BigDecimal soilMoisture;

	/** Temperatura medida en campo (°C). */
	@Column(name = "field_temperature")
	private BigDecimal fieldTemperature;

	/** Precipitación registrada en mm. */
	@Column(name = "precipitation")
	private BigDecimal precipitation;

	/** Radiación solar en W/m². */
	@Column(name = "solar_radiation")
	private BigDecimal solarRadiation;

	/** Velocidad del viento en km/h. */
	@Column(name = "wind_speed")
	private BigDecimal windSpeed;

	/** Tecnología de riego usada (ej: "Goteo", "Aspersión", "Gravedad"). */
	@Column(name = "irrigation_technology", length = 100)
	private String irrigationTechnology;

	// ── c) Técnicas de siembra y desarrollo ───────────────────────────────

	/** Densidad de siembra en plantas por hectárea. */
	@Column(name = "planting_density")
	private BigDecimal plantingDensity;

	/** Variedad de semilla seleccionada. */
	@Column(name = "seed_variety", length = 200)
	private String seedVariety;

	/** Indica si la semilla está adaptada a la zona. */
	@Column(name = "seed_adapted_to_zone")
	private Boolean seedAdaptedToZone;

	/** Inicio de la ventana óptima de siembra. */
	@Column(name = "optimal_sowing_start")
	private LocalDate optimalSowingStart;

	/** Fin de la ventana óptima de siembra. */
	@Column(name = "optimal_sowing_end")
	private LocalDate optimalSowingEnd;

	/** Etapa actual de crecimiento (ej: "Vegetativo", "Floración"). */
	@Column(name = "current_growth_stage", length = 100)
	private String currentGrowthStage;

	// ── Timestamps ────────────────────────────────────────────────────────

	/** Fecha en la que se tomaron las mediciones de campo. */
	@Column(name = "measured_at")
	private LocalDateTime measuredAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
