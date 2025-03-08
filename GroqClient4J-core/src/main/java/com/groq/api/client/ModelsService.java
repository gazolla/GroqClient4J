package com.groq.api.client;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service interface for models operations.
 */
public interface ModelsService {
    /**
     * Lists available models.
     *
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    CompletableFuture<JsonNode> listModels();
}