package com.agromag.repository;

import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.CropType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Acceso a datos de parámetros agronómicos por tipo de cultivo
public interface CropParameterRepository extends JpaRepository<CropParameter, Long> {

	Optional<CropParameter> findByCropType(CropType cropType);

	boolean existsByCropType(CropType cropType);

	List<CropParameter> findAllByOrderByCropTypeAsc();
}
