package com.groq.api.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.groq.api.config.GroqApiConfig;
import com.groq.api.exceptions.GroqApiException;
import com.groq.api.models.Tool;
import com.groq.api.utils.ImageUtils;
import com.groq.api.utils.JsonUtils;

/**
 * Main client for interacting with the Groq API.
 * Provides implementation for all Groq API services.
 */
public class GroqApiClient implements GroqApi {
    private final GroqApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new GroqApiClient with the specified configuration and HTTP client.
     *
     * @param config The configuration for the API client.
     * @param httpClient The HTTP client for making requests.
     * @param objectMapper The JSON object mapper.
     */
    public GroqApiClient(GroqApiConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a chat completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createChatCompletion(JsonNode request) {
        String url = config.getFullUrl(GroqApiConfig.CHAT_COMPLETIONS_ENDPOINT);
        
        return sendJsonRequest(url, request)
            .thenApply(response -> {
                try {
                    JsonNode jsonResponse = objectMapper.readTree(response);
                    checkForErrors(jsonResponse);
                    return jsonResponse;
                } catch (IOException e) {
                    throw new CompletionException("Failed to parse JSON response", e);
                }
            });
    }

    /**
     * Creates a streaming chat completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A publisher that will emit JSON objects as they arrive.
     */
    @Override
    public Publisher<JsonNode> createChatCompletionStream(JsonNode request) {
        // Create a modified request with stream=true
        ObjectNode streamRequest;
        if (request instanceof ObjectNode) {
            streamRequest = (ObjectNode) request.deepCopy();
        } else {
            streamRequest = request.deepCopy();
        }
        streamRequest.put("stream", true);
        
        String url = config.getFullUrl(GroqApiConfig.CHAT_COMPLETIONS_ENDPOINT);
        SubmissionPublisher<JsonNode> publisher = new SubmissionPublisher<>();
        
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(streamRequest)))
                    .build();
                
                httpClient.send(
                    httpRequest, 
                    HttpResponse.BodyHandlers.ofLines()
                ).body().forEach(line -> {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6); // Remove "data: " prefix
                        if (!"[DONE]".equals(data)) {
                            try {
                                publisher.submit(objectMapper.readTree(data));
                            } catch (IOException e) {
                                publisher.closeExceptionally(new GroqApiException(
                                    -1, 
                                    "Failed to parse JSON chunk: " + data,
                                    e
                                ));
                            }
                        }
                    }
                });
                
                publisher.close();
            } catch (Exception e) {
                publisher.closeExceptionally(new GroqApiException(
                    -1, 
                    "Stream request failed: " + e.getMessage(),
                    e
                ));
            }
        });
        
        return publisher;
    }

    /**
     * Creates a transcription from an audio file.
     *
     * @param audioFile The audio file input stream.
     * @param fileName The name of the audio file.
     * @param model The model to use for transcription.
     * @param prompt Optional prompt to guide the transcription.
     * @param responseFormat The format of the response (default: "json").
     * @param language Optional language of the audio.
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createTranscription(
            InputStream audioFile,
            String fileName,
            String model,
            String prompt,
            String responseFormat,
            String language,
            Float temperature) {
        
        String url = config.getFullUrl(GroqApiConfig.TRANSCRIPTIONS_ENDPOINT);
        
        return sendMultipartRequest(url, () -> {
            try {
                MultipartBodyPublisher publisher = new MultipartBodyPublisher()
                    .addPart("file", fileName, audioFile)
                    .addPart("model", model);
                
                if (prompt != null && !prompt.isBlank()) {
                    publisher.addPart("prompt", prompt);
                }
                
                publisher.addPart("response_format", responseFormat);
                
                if (language != null && !language.isBlank()) {
                    publisher.addPart("language", language);
                }
                
                if (temperature != null) {
                    publisher.addPart("temperature", temperature.toString());
                }
                
                return publisher.build();
            } catch (IOException e) {
                throw new CompletionException("Failed to create multipart request", e);
            }
        });
    }

    /**
     * Creates a translation from an audio file.
     *
     * @param audioFile The audio file input stream.
     * @param fileName The name of the audio file.
     * @param model The model to use for translation.
     * @param prompt Optional prompt to guide the translation.
     * @param responseFormat The format of the response (default: "json").
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createTranslation(
            InputStream audioFile,
            String fileName,
            String model,
            String prompt,
            String responseFormat,
            Float temperature) {
        
        String url = config.getFullUrl(GroqApiConfig.TRANSLATIONS_ENDPOINT);
        
        return sendMultipartRequest(url, () -> {
            try {
                MultipartBodyPublisher publisher = new MultipartBodyPublisher()
                    .addPart("file", fileName, audioFile)
                    .addPart("model", model);
                
                if (prompt != null && !prompt.isBlank()) {
                    publisher.addPart("prompt", prompt);
                }
                
                publisher.addPart("response_format", responseFormat);
                
                if (temperature != null) {
                    publisher.addPart("temperature", temperature.toString());
                }
                
                return publisher.build();
            } catch (IOException e) {
                throw new CompletionException("Failed to create multipart request", e);
            }
        });
    }

    /**
     * Creates a vision completion with the provided request.
     *
     * @param request JSON object containing the request parameters.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createVisionCompletion(JsonNode request) {
        try {
            ImageUtils.validateVisionModel(request);
            return createChatCompletion(request);
        } catch (GroqApiException e) {
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Creates a vision completion with an image URL.
     *
     * @param imageUrl The URL of the image to analyze.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createVisionCompletionWithImageUrl(
            String imageUrl,
            String prompt,
            String model,
            Float temperature) {
        
        try {
            ImageUtils.validateImageUrl(imageUrl);
            
            JsonNode request = JsonUtils.createVisionRequestWithUrl(
                imageUrl,
                prompt,
                model,
                temperature
            );
            
            return createVisionCompletion(request);
        } catch (GroqApiException e) {
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Creates a vision completion with a base64-encoded image.
     *
     * @param imagePath The path to the image file.
     * @param prompt The text prompt for the model.
     * @param model The vision model to use.
     * @param temperature Optional temperature parameter for generation.
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> createVisionCompletionWithBase64Image(
            String imagePath,
            String prompt,
            String model,
            Float temperature) {
        
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        
        try {
            String base64Image = ImageUtils.convertImageToBase64(imagePath);
            ImageUtils.validateBase64Size(base64Image, config.maxBase64SizeMB());
            
            JsonNode request = JsonUtils.createVisionRequestWithBase64(
                base64Image,
                prompt,
                model,
                temperature
            );
            
            return createVisionCompletion(request);
        } catch (Exception e) {
            future.completeExceptionally(e instanceof GroqApiException
                ? (GroqApiException) e
                : new GroqApiException(400, "Error processing image: " + e.getMessage(), e));
        }
        
        return future;
    }

    /**
     * Runs a conversation with tools.
     *
     * @param userPrompt The user's prompt or message.
     * @param tools The list of tools to make available.
     * @param model The model to use.
     * @param systemMessage Optional system message.
     * @return A CompletableFuture that will complete with the response text.
     */
    @Override
    public CompletableFuture<String> runConversationWithTools(
            String userPrompt,
            List<Tool> tools,
            String model,
            String systemMessage) {
        
        List<JsonNode> messages = new ArrayList<>();
        
        // Add system message if provided
        if (systemMessage != null && !systemMessage.isBlank()) {
            ObjectNode systemNode = objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", systemMessage);
            messages.add(systemNode);
        }
        
        // Add user message
        ObjectNode userNode = objectMapper.createObjectNode()
            .put("role", "user")
            .put("content", userPrompt);
        messages.add(userNode);
        
        // Create tool request
        JsonNode request = JsonUtils.createToolsRequest(model, messages, tools, 0.7f);
        
        // First API call to get tool calls
        return createChatCompletion(request)
            .thenCompose(response -> {
                JsonNode choices = response.path("choices");
                if (choices.isEmpty() || choices.size() == 0) {
                    return CompletableFuture.completedFuture(null);
                }
                
                JsonNode message = choices.path(0).path("message");
                JsonNode toolCalls = message.path("tool_calls");
                
                // If no tool calls, just return the content
                if (toolCalls.isEmpty() || toolCalls.size() == 0) {
                    return CompletableFuture.completedFuture(
                        message.path("content").asText("")
                    );
                }
                
                // Add assistant message with tool calls to conversation
                messages.add(message);
                
                // Process tool calls
                List<CompletableFuture<JsonNode>> toolResponses = new ArrayList<>();
                
                for (JsonNode toolCall : toolCalls) {
                    String functionName = toolCall.path("function").path("name").asText();
                    String arguments = toolCall.path("function").path("arguments").asText();
                    String toolCallId = toolCall.path("id").asText();
                    
                    // Find matching tool
                    tools.stream()
                        .filter(tool -> tool.function().name().equals(functionName))
                        .findFirst()
                        .ifPresent(tool -> {
                            // Execute the tool and add response to messages
                            CompletableFuture<JsonNode> toolResponse = tool.function().executor()
                                .execute(arguments)
                                .thenApply(result -> {
                                    JsonNode responseMessage = JsonUtils.createToolResponseMessage(
                                        toolCallId, 
                                        functionName, 
                                        result
                                    );
                                    // Add tool response to messages
                                    messages.add(responseMessage);
                                    return responseMessage;
                                });
                            
                            toolResponses.add(toolResponse);
                        });
                }
                
                // Wait for all tool responses to complete
                return CompletableFuture.allOf(
                    toolResponses.toArray(new CompletableFuture[0])
                ).thenCompose(v -> {
                    // Create second request with updated messages
                    JsonNode secondRequest = JsonUtils.createChatRequest(model, messages, 0.7f);
                    
                    // Make second API call to get final response
                    return createChatCompletion(secondRequest)
                        .thenApply(secondResponse -> 
                            JsonUtils.extractContentFromCompletion(secondResponse)
                        );
                });
            });
    }

    /**
     * Lists available models.
     *
     * @return A CompletableFuture that will complete with the response JSON object.
     */
    @Override
    public CompletableFuture<JsonNode> listModels() {
        String url = config.getFullUrl(GroqApiConfig.MODELS_ENDPOINT);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + config.apiKey())
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    try {
                        return objectMapper.readTree(response.body());
                    } catch (IOException e) {
                        throw new CompletionException("Failed to parse response: " + response.body(), e);
                    }
                } else {
                    throw new CompletionException(new GroqApiException(
                        response.statusCode(),
                        "API request failed with status " + response.statusCode() + ": " + response.body()
                    ));
                }
            });
    }

    /**
     * Closes the client and releases resources.
     */
    @Override
    public void close() {
        // HttpClient doesn't need explicit closing in Java
    }

    //------------------------------------------------------------------------
    // Helper methods
    //------------------------------------------------------------------------

    private CompletableFuture<String> sendJsonRequest(String url, JsonNode body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    } else {
                        throw new CompletionException(new GroqApiException(
                            response.statusCode(),
                            "API request failed with status " + response.statusCode() + ": " + response.body()
                        ));
                    }
                });
        } catch (JsonProcessingException e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new GroqApiException(
                400,
                "Failed to serialize request body",
                e
            ));
            return future;
        }
    }

    private CompletableFuture<JsonNode> sendMultipartRequest(String url, Supplier<BodyPublisher> bodySupplier) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(bodySupplier.get())
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            return objectMapper.readTree(response.body());
                        } catch (IOException e) {
                            throw new CompletionException("Failed to parse response: " + response.body(), e);
                        }
                    } else {
                        throw new CompletionException(new GroqApiException(
                            response.statusCode(),
                            "API request failed with status " + response.statusCode() + ": " + response.body()
                        ));
                    }
                });
        } catch (Exception e) {
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            future.completeExceptionally(new GroqApiException(
                400,
                "Failed to create request: " + e.getMessage(),
                e
            ));
            return future;
        }
    }

    private void checkForErrors(JsonNode response) {
        JsonNode error = response.path("error");
        if (!error.isMissingNode()) {
            String message = error.path("message").asText("Unknown error");
            String errorType = error.path("type").asText(null);
            String errorCode = error.path("code").asText(null);
            
            throw new CompletionException(new GroqApiException(
                400,
                "API error: " + message,
                errorType,
                errorCode,
                null
            ));
        }
    }

    /**
     * Custom exception for CompletableFuture operations.
     */
    private static class CompletionException extends RuntimeException {
        public CompletionException(String message) {
            super(message);
        }
        
        public CompletionException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public CompletionException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Helper class for building multipart requests.
     */
    private static class MultipartBodyPublisher {
        private final String boundary = "----GroqApiClientBoundary" + System.currentTimeMillis();
        private final List<byte[]> parts = new ArrayList<>();
        private final String lineBreak = "\r\n";
        
        public MultipartBodyPublisher addPart(String name, String value) {
            StringBuilder builder = new StringBuilder();
            builder.append("--").append(boundary).append(lineBreak);
            builder.append("Content-Disposition: form-data; name=\"").append(name).append("\"");
            builder.append(lineBreak).append(lineBreak);
            builder.append(value).append(lineBreak);
            parts.add(builder.toString().getBytes(StandardCharsets.UTF_8));
            return this;
        }
        
        public MultipartBodyPublisher addPart(String name, String filename, InputStream data) throws IOException {
            StringBuilder builder = new StringBuilder();
            builder.append("--").append(boundary).append(lineBreak);
            builder.append("Content-Disposition: form-data; name=\"").append(name)
                   .append("\"; filename=\"").append(filename).append("\"");
            builder.append(lineBreak);
            builder.append("Content-Type: application/octet-stream");
            builder.append(lineBreak).append(lineBreak);
            parts.add(builder.toString().getBytes(StandardCharsets.UTF_8));
            
            // Add file data
            parts.add(data.readAllBytes());
            parts.add(lineBreak.getBytes(StandardCharsets.UTF_8));
            return this;
        }
        
        public BodyPublisher build() {
            // Add closing boundary
            parts.add(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
            
            // Calculate total length
            long contentLength = parts.stream()
                .mapToLong(part -> part.length)
                .sum();
            
            // Create publisher
            return BodyPublishers.ofByteArrays(parts);
        }
        
        public String getContentType() {
            return "multipart/form-data; boundary=" + boundary;
        }
    }
}