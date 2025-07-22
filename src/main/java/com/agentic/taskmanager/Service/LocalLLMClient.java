package com.agentic.taskmanager.Service;

import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.cache.annotation.Cacheable;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocalLLMClient {
    private final WebClient webClient;

    // Corrected Constructor: Spring will inject the value of ollama.api.url
    public LocalLLMClient(@Value("${ollama.api.url}") String ollamaApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ollamaApiUrl) // Use the injected String, not the placeholder
                .build();
    }
    @Cacheable(value = "ollamaResponses", key = "#prompt") // Cache results of this method
    public String askLLM(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "llama3:latest", // or "llama3", etc.
                "prompt", prompt,
                "stream", false
        );

        // If your ollama.api.url already includes /api/generate, then this .uri() call
        // might be redundant or incorrect. Your application.properties has /api/generate,
        // so you might want to remove it here.
        // For example: ollama.api.url=http://ollama:11434
        // Then: .uri("/api/generate")
        // But if ollama.api.url=http://ollama:11434/api/generate
        // Then: .uri("") or .uri("/") might be needed.
        // Let's assume for now that application.properties will be http://ollama:11434
        return webClient.post()
                .uri("/api/generate") // Keep this if ollama.api.url is just the base
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // Use HttpHeaders constants
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}