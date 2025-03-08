package com.groq.api.client;

import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groq.api.config.GroqApiConfig;

/**
 * Factory for creating Groq API clients and related components.
 */
public final class GroqClientFactory {
    private GroqClientFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Creates a default HTTP client for the Groq API.
     * 
     * @return A configured HttpClient instance.
     */
    public static HttpClient createDefaultHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    /**
     * Creates a new Groq API client with the provided API key.
     * 
     * @param apiKey The API key to use for authentication.
     * @return A configured GroqApiClient instance.
     */
    public static GroqApiClient createClient(String apiKey) {
        GroqApiConfig config = GroqApiConfig.create(apiKey);
        return createClient(config);
    }
    
    /**
     * Creates a new Groq API client with the provided configuration.
     * 
     * @param config The configuration for the client.
     * @return A configured GroqApiClient instance.
     */
    public static GroqApiClient createClient(GroqApiConfig config) {
        return createClient(config, createDefaultHttpClient(), new ObjectMapper());
    }
    
    /**
     * Creates a new Groq API client with the provided configuration and HTTP client.
     * 
     * @param config The configuration for the client.
     * @param httpClient Custom HTTP client.
     * @param objectMapper JSON object mapper.
     * @return A configured GroqApiClient instance.
     */
    public static GroqApiClient createClient(GroqApiConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        return new GroqApiClient(config, httpClient, objectMapper);
    }
}