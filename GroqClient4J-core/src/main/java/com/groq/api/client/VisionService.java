package com.groq.api.client;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service interface for vision operations.
 */
public interface VisionService {
    /**
     * Creates a vision completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    CompletableFuture<JsonNode> createVisionCompletion(JsonNode request);
    
    /**
     * Creates a vision completion with an image URL.
     *
     * @param imageUrl The URL of the image to analyze.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    CompletableFuture<JsonNode> createVisionCompletionWithImageUrl(
        String imageUrl,
        String prompt,
        String model,
        Float temperature
    );
    
    /**
     * Creates a vision completion with a base64-encoded image.
     *
     * @param imagePath The path to the image file.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    CompletableFuture<JsonNode> createVisionCompletionWithBase64Image(
        String imagePath,
        String prompt,
        String model,
        Float temperature
    );
}