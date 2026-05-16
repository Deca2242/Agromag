package com.agromag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< Updated upstream
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
=======
<<<<<<< Updated upstream

@SpringBootApplication
=======
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
>>>>>>> Stashed changes
>>>>>>> Stashed changes
public class AgromagApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgromagApplication.class, args);
	}
}
