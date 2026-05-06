package com.agromag.repository;

import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.CropType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Acceso a datos de parámetros agronómicos por tipo de cultivo
public interface CropParameterRepository extends JpaRepository<CropParameter, Long> {

	Optional<CropParameter> findByCropType(CropType cropType);

	boolean existsByCropType(CropType cropType);

	List<CropParameter> findAllByOrderByCropTypeAsc();

	// Cultivos cuyo rango de temperatura óptima incluya la temperatura dada
	@Query("SELECT cp FROM CropParameter cp WHERE cp.optimalTempMin <= :temperature AND cp.optimalTempMax >= :temperature")
	List<CropParameter> findByOptimalTemperatureRange(BigDecimal temperature);

	// Cultivos cuyo rango de humedad óptima incluya la humedad dada
	@Query("SELECT cp FROM CropParameter cp WHERE cp.humidityMin <= :humidity AND cp.humidityMax >= :humidity")
	List<CropParameter> findByOptimalHumidityRange(BigDecimal humidity);

	// Cultivos compatibles con temperatura Y humedad dadas
	@Query("SELECT cp FROM CropParameter cp WHERE cp.optimalTempMin <= :temperature AND cp.optimalTempMax >= :temperature AND cp.humidityMin <= :humidity AND cp.humidityMax >= :humidity")
	List<CropParameter> findByClimateCompatibility(BigDecimal temperature, BigDecimal humidity);
}
