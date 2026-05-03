package com.agromag;

import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.CropType;
import com.agromag.repository.CropParameterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class AgromagApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgromagApplication.class, args);
	}

	@Bean
	CommandLineRunner initCropParameters(CropParameterRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				List<CropParameter> defaultParams = List.of(
						createParam(CropType.BANANO, "2.5m x 2.5m", 270, 26.0, 30.0, 75.0, 85.0, 5.5, 7.0, 1.0, 2.0, "20-25 mm/day", "NPK 15-5-20", 30.0),
						createParam(CropType.MANGO, "8m x 8m", 150, 24.0, 30.0, 50.0, 70.0, 5.5, 7.5, 0.5, 1.5, "100-150 L/week", "NPK 10-20-20", 50.0),
						createParam(CropType.YUCA, "1m x 1m", 300, 25.0, 29.0, 60.0, 80.0, 5.5, 6.5, 0.5, 1.0, "10-15 mm/day", "NPK 12-12-17", 15.0),
						createParam(CropType.PLATANO, "3m x 2m", 360, 25.0, 30.0, 70.0, 85.0, 5.5, 7.0, 1.0, 2.0, "20-30 mm/day", "NPK 14-4-28", 40.0),
						createParam(CropType.MAIZ, "0.8m x 0.2m", 120, 20.0, 30.0, 50.0, 70.0, 5.8, 7.0, 1.5, 2.5, "5-10 mm/day", "Urea 46-0-0", 5.0),
						createParam(CropType.PALMA, "9m x 9m", 1095, 24.0, 32.0, 80.0, 90.0, 4.0, 6.0, 1.0, 2.0, "150-200 L/day", "NPKMg 12-12-17-2", 45.0)
				);
				repository.saveAll(defaultParams);
			}
		};
	}

	private CropParameter createParam(CropType type, String spacing, int cycle, double tempMin, double tempMax, double humMin, double humMax, double phMin, double phMax, double ecMin, double ecMax, String irrNeeds, String fert, double depth) {
		CropParameter p = new CropParameter();
		p.setCropType(type);
		p.setSuggestedSpacing(spacing);
		p.setGrowthCycleDays(cycle);
		p.setOptimalTempMin(BigDecimal.valueOf(tempMin));
		p.setOptimalTempMax(BigDecimal.valueOf(tempMax));
		p.setHumidityMin(BigDecimal.valueOf(humMin));
		p.setHumidityMax(BigDecimal.valueOf(humMax));
		p.setPhMin(BigDecimal.valueOf(phMin));
		p.setPhMax(BigDecimal.valueOf(phMax));
		p.setEcMin(BigDecimal.valueOf(ecMin));
		p.setEcMax(BigDecimal.valueOf(ecMax));
		p.setIrrigationNeeds(irrNeeds);
		p.setRecommendedFertilizer(fert);
		p.setPlantingDepthCm(BigDecimal.valueOf(depth));
		return p;
	}
}
