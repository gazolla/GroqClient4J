package com.groq.api.models;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a function that can be used by the Groq API.
 */
public record Function(
    String name,
    String description,
    JsonNode parameters,
    FunctionExecutor executor
) {
    /**
     * Functional interface for executing functions.
     */
    @FunctionalInterface
    public interface FunctionExecutor {
        /**
         * Executes a function with the given arguments.
         *
         * @param arguments The JSON arguments as a string.
         * @return A CompletableFuture containing the response as a string.
         */
        CompletableFuture<String> execute(String arguments);
    }
}