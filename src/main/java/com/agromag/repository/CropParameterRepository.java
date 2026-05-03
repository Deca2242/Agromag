package com.agromag.repository;

import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.CropType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CropParameterRepository extends JpaRepository<CropParameter, Long> {

	Optional<CropParameter> findByCropType(CropType cropType);

	boolean existsByCropType(CropType cropType);

	List<CropParameter> findAllByOrderByCropTypeAsc();

	/**
	 * Busca cultivos cuyo rango de temperatura óptima incluya la temperatura dada.
	 * Útil para recomendar qué cultivar según el clima actual.
	 */
	@Query("SELECT cp FROM CropParameter cp WHERE cp.optimalTempMin <= :temperature AND cp.optimalTempMax >= :temperature")
	List<CropParameter> findByOptimalTemperatureRange(BigDecimal temperature);

	/**
	 * Busca cultivos cuyo rango de humedad óptima incluya la humedad dada.
	 */
	@Query("SELECT cp FROM CropParameter cp WHERE cp.humidityMin <= :humidity AND cp.humidityMax >= :humidity")
	List<CropParameter> findByOptimalHumidityRange(BigDecimal humidity);

	/**
	 * Busca cultivos compatibles con las condiciones climáticas actuales
	 * (temperatura Y humedad dentro de rangos óptimos).
	 */
	@Query("SELECT cp FROM CropParameter cp WHERE cp.optimalTempMin <= :temperature AND cp.optimalTempMax >= :temperature AND cp.humidityMin <= :humidity AND cp.humidityMax >= :humidity")
	List<CropParameter> findByClimateCompatibility(BigDecimal temperature, BigDecimal humidity);
}
