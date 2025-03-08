package com.groq.api.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service interface for chat completion operations.
 */
public interface ChatCompletionService {
    /**
     * Creates a chat completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    CompletableFuture<JsonNode> createChatCompletion(JsonNode request);
    
    /**
     * Creates a streaming chat completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A publisher that will emit JSON objects as they arrive.
     */
    Flow.Publisher<JsonNode> createChatCompletionStream(JsonNode request);
}