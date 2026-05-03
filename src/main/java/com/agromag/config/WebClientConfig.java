package com.agromag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Value("${openmeteo.url}")
	private String openMeteoBaseUrl;

	@Bean
	public WebClient openMeteoWebClient() {
		return WebClient.builder()
				.baseUrl(openMeteoBaseUrl)
				.build();
	}
}
