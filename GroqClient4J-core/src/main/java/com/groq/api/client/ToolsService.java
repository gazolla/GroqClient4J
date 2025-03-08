package com.groq.api.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.groq.api.models.Tool;

/**
 * Service interface for tools operations.
 */
public interface ToolsService {
    /**
     * Runs a conversation with tools.
     *
     * @param userPrompt The user's prompt or message.
     * @param tools The list of tools to make available.
     * @param model The model to use.
     * @param systemMessage Optional system message.
     * @return A CompletableFuture that will complete with the response text.
     */
    CompletableFuture<String> runConversationWithTools(
        String userPrompt,
        List<Tool> tools,
        String model,
        String systemMessage
    );
}