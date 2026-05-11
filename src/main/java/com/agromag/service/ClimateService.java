package com.agromag.service;

import com.agromag.domain.enums.Municipality;
import com.agromag.domain.model.ClimateData;
import com.agromag.exception.ClimateServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

// Consulta la API de Open-Meteo para obtener datos climáticos actuales de un municipio
@Service
public class ClimateService {

	private static final Logger log = LoggerFactory.getLogger(ClimateService.class);

	private final WebClient openMeteoWebClient;
	private final ObjectMapper objectMapper;

	public ClimateService(WebClient openMeteoWebClient, ObjectMapper objectMapper) {
		this.openMeteoWebClient = openMeteoWebClient;
		this.objectMapper = objectMapper;
	}

	// Obtiene temperatura y humedad actual del municipio usando sus coordenadas geográficas
	public ClimateData getCurrentClimate(Municipality municipality) {
		try {
			log.debug("fetch_climate municipality={}", municipality.getDisplayName());

			String responseString = openMeteoWebClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("latitude", municipality.getLatitude())
							.queryParam("longitude", municipality.getLongitude())
							.queryParam("current", "temperature_2m,relative_humidity_2m")
							.build())
					.retrieve()
					.bodyToMono(String.class)
					.block();

			if (responseString == null) {
				throw new ClimateServiceException("Respuesta nula de Open-Meteo para " + municipality.getDisplayName());
			}

			JsonNode response = objectMapper.readTree(responseString);

			if (!response.has("current")) {
				throw new ClimateServiceException(
						"Respuesta inválida de Open-Meteo para " + municipality.getDisplayName());
			}

			JsonNode current = response.get("current");
			BigDecimal temperature = BigDecimal.valueOf(current.get("temperature_2m").asDouble());
			BigDecimal humidity = BigDecimal.valueOf(current.get("relative_humidity_2m").asDouble());

			log.info("climate_result municipality={} temp={} hum={}", municipality.getDisplayName(), temperature, humidity);
			return new ClimateData(temperature, humidity);

		} catch (Exception e) {
			if (e instanceof ClimateServiceException cse) throw cse;
			throw new ClimateServiceException(
					"Error al consultar Open-Meteo para " + municipality.getDisplayName(), e);
		}
	}
}
