package com.agromag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agromag.web-search")
public record WebSearchProperties(
		boolean enabled,
		String apiKey,
		String url,
		int maxResults,
		int timeoutSeconds
) {
	public boolean isConfigured() {
		return enabled && apiKey != null && !apiKey.isBlank();
	}

	public String urlOrDefault() {
		return url == null || url.isBlank() ? "https://api.tavily.com/search" : url;
	}

	public int maxResultsOrDefault() {
		return maxResults <= 0 ? 4 : Math.min(maxResults, 6);
	}

	public int timeoutSecondsOrDefault() {
		return timeoutSeconds <= 0 ? 8 : Math.min(timeoutSeconds, 15);
	}
}
