package com.agromag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgromagApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgromagApplication.class, args);
	}
}
