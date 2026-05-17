package com.agromag.service;

import com.agromag.config.WebSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WebSearchService {

	private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

	private final WebSearchProperties properties;
	private final WebClient webClient;

	public WebSearchService(WebSearchProperties properties) {
		this.properties = properties;
		this.webClient = WebClient.builder().build();
	}

	public boolean isAvailable() {
		return properties.isConfigured();
	}

	public WebSearchSummary search(String query) {
		if (!isAvailable()) {
			return WebSearchSummary.disabled();
		}
		if (query == null || query.isBlank()) {
			return WebSearchSummary.empty("Consulta vacía.");
		}

		try {
			Map<String, Object> request = Map.of(
					"api_key", properties.apiKey(),
					"query", query,
					"search_depth", "basic",
					"max_results", properties.maxResultsOrDefault(),
					"include_answer", false,
					"include_raw_content", false
			);

			JsonNode response = webClient.post()
					.uri(properties.urlOrDefault())
					.bodyValue(request)
					.retrieve()
					.bodyToMono(JsonNode.class)
					.timeout(Duration.ofSeconds(properties.timeoutSecondsOrDefault()))
					.block();

			List<WebSearchResult> results = parseResults(response);
			log.info("web_search_done queryLength={} results={}", query.length(), results.size());
			return results.isEmpty()
					? WebSearchSummary.empty("No se encontraron resultados útiles.")
					: new WebSearchSummary(true, null, results);
		} catch (Exception e) {
			log.warn("web_search_failed error={}", e.getMessage());
			return WebSearchSummary.empty("No se pudo consultar internet en este momento.");
		}
	}

	private List<WebSearchResult> parseResults(JsonNode response) {
		List<WebSearchResult> results = new ArrayList<>();
		if (response == null || !response.has("results") || !response.get("results").isArray()) {
			return results;
		}

		for (JsonNode item : response.get("results")) {
			String title = text(item, "title");
			String url = text(item, "url");
			String content = text(item, "content");
			if (title.isBlank() && content.isBlank()) {
				continue;
			}
			results.add(new WebSearchResult(title, url, trim(content, 500)));
		}
		return results;
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? "" : value.asText("").trim();
	}

	private String trim(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value == null ? "" : value;
		}
		return value.substring(0, maxLength).trim() + "...";
	}

	public record WebSearchSummary(boolean searched, String note, List<WebSearchResult> results) {
		static WebSearchSummary disabled() {
			return new WebSearchSummary(false, "Búsqueda web deshabilitada o sin API key.", List.of());
		}

		static WebSearchSummary empty(String note) {
			return new WebSearchSummary(true, note, List.of());
		}
	}

	public record WebSearchResult(String title, String url, String content) {
	}
}
