package com.groq.api.utils;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.groq.api.models.Tool;

/**
 * Utility class for working with JSON in the Groq API.
 */
public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private JsonUtils() {
        // Utility class should not be instantiated
    }
    
    /**
     * Extracts the content of text from a chat completion response.
     *
     * @param response The JSON response from the chat completion API.
     * @return The extracted content as a string, or null if not found.
     */
    public static String extractContentFromCompletion(JsonNode response) {
        if (response == null) {
            return null;
        }
        
        return response
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText(null);
    }

    /**
     * Extracts the content of text from a streaming chunk.
     *
     * @param chunk The JSON chunk from a streaming response.
     * @return The extracted content as a string, or null if not found.
     */
    public static String extractContentFromChunk(JsonNode chunk) {
        if (chunk == null) {
            return null;
        }
        
        return chunk
            .path("choices")
            .path(0)
            .path("delta")
            .path("content")
            .asText(null);
    }

    /**
     * Creates a chat request with multiple messages.
     *
     * @param model The model to use for the request.
     * @param messages The list of messages as JsonNodes.
     * @param temperature The temperature parameter for generation.
     * @return A JSON object representing the request.
     */
    public static JsonNode createChatRequest(
        String model,
        List<JsonNode> messages,
        float temperature
    ) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("model", model);
        
        ArrayNode messagesArray = request.putArray("messages");
        messages.forEach(messagesArray::add);
        
        request.put("temperature", temperature);
        
        return request;
    }

    /**
     * Creates a simple chat request with a single user message.
     *
     * @param model The model to use for the request.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @param temperature The temperature parameter for generation.
     * @return A JSON object representing the request.
     */
    public static JsonNode createSimpleChatRequest(
        String model,
        String userMessage,
        String systemMessage,
        float temperature
    ) {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode userMessageNode = mapper.createObjectNode()
            .put("role", "user")
            .put("content", userMessage);
        
        if (systemMessage == null || systemMessage.isBlank()) {
            return createChatRequest(
                model, 
                List.of(userMessageNode), 
                temperature
            );
        }
        
        ObjectNode systemMessageNode = mapper.createObjectNode()
            .put("role", "system")
            .put("content", systemMessage);
            
        return createChatRequest(
            model, 
            List.of(systemMessageNode, userMessageNode), 
            temperature
        );
    }

    /**
     * Creates a vision request with an image URL.
     *
     * @param imageUrl The URL of the image to analyze.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature The temperature parameter for generation.
     * @return A JSON object representing the request.
     */
    public static JsonNode createVisionRequestWithUrl(
        String imageUrl,
        String prompt,
        String model,
        Float temperature
    ) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("model", model);
        
        ArrayNode messagesArray = request.putArray("messages");
        ObjectNode userMessage = messagesArray.addObject()
            .put("role", "user");
        
        ArrayNode contentArray = userMessage.putArray("content");
        contentArray.addObject()
            .put("type", "text")
            .put("text", prompt);
            
        ObjectNode imageUrlObject = contentArray.addObject()
            .put("type", "image_url");
            
        imageUrlObject.putObject("image_url")
            .put("url", imageUrl);
        
        if (temperature != null) {
            request.put("temperature", temperature);
        }
        
        return request;
    }

    /**
     * Creates a vision request with a base64-encoded image.
     *
     * @param base64Image The base64-encoded image data.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature The temperature parameter for generation.
     * @return A JSON object representing the request.
     */
    public static JsonNode createVisionRequestWithBase64(
        String base64Image,
        String prompt,
        String model,
        Float temperature
    ) {
        return createVisionRequestWithUrl(
            "data:image/jpeg;base64," + base64Image,
            prompt,
            model,
            temperature
        );
    }

    /**
     * Creates a request for using tools.
     *
     * @param model The model to use.
     * @param messages The conversation messages.
     * @param tools The list of tools to make available.
     * @param temperature The temperature parameter for generation.
     * @return A JSON object representing the request.
     */
    public static JsonNode createToolsRequest(
        String model,
        List<JsonNode> messages,
        List<Tool> tools,
        float temperature
    ) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("model", model);
        
        ArrayNode messagesArray = request.putArray("messages");
        messages.forEach(messagesArray::add);
        
        ArrayNode toolsArray = request.putArray("tools");
        tools.forEach(tool -> {
            ObjectNode toolNode = toolsArray.addObject()
                .put("type", tool.type());
                
            ObjectNode functionNode = toolNode.putObject("function")
                .put("name", tool.function().name())
                .put("description", tool.function().description());
                
            functionNode.set("parameters", tool.function().parameters());
        });
        
        request.put("tool_choice", "auto");
        request.put("temperature", temperature);
        
        return request;
    }

    /**
     * Creates a message representing a tool's response.
     *
     * @param toolCallId The ID of the tool call.
     * @param functionName The name of the function that was called.
     * @param functionResponse The response from the function.
     * @return A JSON object representing the tool's response message.
     */
    public static JsonNode createToolResponseMessage(
        String toolCallId,
        String functionName,
        String functionResponse
    ) {
        return MAPPER.createObjectNode()
            .put("tool_call_id", toolCallId)
            .put("role", "tool")
            .put("name", functionName)
            .put("content", functionResponse);
    }
}