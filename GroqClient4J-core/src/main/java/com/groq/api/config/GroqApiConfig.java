package com.groq.api.config;

import java.util.Set;

/**
 * Configuration class for the Groq API client.
 */
public record GroqApiConfig(
    String apiKey,
    String baseUrl,
    int maxBase64SizeMB
) {
    public static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    public static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    public static final String TRANSCRIPTIONS_ENDPOINT = "/audio/transcriptions";
    public static final String TRANSLATIONS_ENDPOINT = "/audio/translations";
    public static final String MODELS_ENDPOINT = "/models";
    
    // Vision models
    public static final String VISION_MODEL_90B = "llama-3.2-90b-vision-preview";
    public static final String VISION_MODEL_11B = "llama-3.2-11b-vision-preview";
    public static final Set<String> VISION_MODELS = Set.of(VISION_MODEL_90B, VISION_MODEL_11B);
    
    // Size limits
    public static final int MAX_IMAGE_SIZE_MB = 20;
    public static final int MAX_BASE64_SIZE_MB = 4;
    
    /**
     * Creates a config with default values for baseUrl and maxBase64SizeMB.
     *
     * @param apiKey The API key for authentication.
     * @return A new GroqApiConfig instance.
     */
    public static GroqApiConfig create(String apiKey) {
        return new GroqApiConfig(apiKey, DEFAULT_BASE_URL, MAX_BASE64_SIZE_MB);
    }
    
    /**
     * Returns the full URL for the specified endpoint.
     *
     * @param endpoint The API endpoint to append to the base URL.
     * @return The complete URL for the specified endpoint.
     */
    public String getFullUrl(String endpoint) {
        return baseUrl + endpoint;
    }
}