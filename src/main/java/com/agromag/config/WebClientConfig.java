package com.agromag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.time.Duration;

// Configura el WebClient para consumir la API de Open-Meteo
@Configuration
@EnableConfigurationProperties(OpenMeteoProperties.class)
public class WebClientConfig {

	private final OpenMeteoProperties openMeteoProperties;

	public WebClientConfig(OpenMeteoProperties openMeteoProperties) {
		this.openMeteoProperties = openMeteoProperties;
	}

	@Bean
	public WebClient openMeteoWebClient() {
		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(5));

		return WebClient.builder()
				.baseUrl(openMeteoProperties.url())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	/** Registra el ObjectMapper de Jackson como bean inyectable en el contexto. */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
}
