package com.agromag.service;

import com.agromag.domain.enums.Municipality;
import com.agromag.exception.ClimateServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

/**
 * Servicio que consulta la API de Open-Meteo para obtener
 * datos climáticos actuales (temperatura y humedad) de un municipio.
 */
@Service
public class ClimateService {

	private final WebClient openMeteoWebClient;

	public ClimateService(WebClient openMeteoWebClient) {
		this.openMeteoWebClient = openMeteoWebClient;
	}

	/**
	 * Obtiene la temperatura y humedad actual del municipio
	 * usando las coordenadas precargadas en el enum Municipality.
	 *
	 * Endpoint: GET /v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m
	 */
	public ClimateData getCurrentClimate(Municipality municipality) {
		try {
			JsonNode response = openMeteoWebClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("latitude", municipality.getLatitude())
							.queryParam("longitude", municipality.getLongitude())
							.queryParam("current", "temperature_2m,relative_humidity_2m")
							.build())
					.retrieve()
					.bodyToMono(JsonNode.class)
					.block();

			if (response == null || !response.has("current")) {
				throw new ClimateServiceException(
						"Respuesta inválida de Open-Meteo para " + municipality.getDisplayName());
			}

			JsonNode current = response.get("current");
			BigDecimal temperature = BigDecimal.valueOf(current.get("temperature_2m").asDouble());
			BigDecimal humidity = BigDecimal.valueOf(current.get("relative_humidity_2m").asDouble());

			return new ClimateData(temperature, humidity);

		} catch (ClimateServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ClimateServiceException(
					"Error al consultar Open-Meteo para " + municipality.getDisplayName(), e);
		}
	}
}
