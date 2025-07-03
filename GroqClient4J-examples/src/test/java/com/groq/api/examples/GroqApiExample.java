package com.groq.api.examples;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.groq.api.client.GroqApiClient;
import com.groq.api.client.GroqClientFactory;
import com.groq.api.extensions.GroqApiExtensions;
import com.groq.api.models.Function;
import com.groq.api.models.Tool;

/**
 * Example demonstrating the usage of the Groq API client.
 */
public class GroqApiExample {

    private static final String API_KEY = System.getenv("GROQ_API_KEY");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public static void main(String[] args) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("Please set the GROQ_API_KEY environment variable");
            System.exit(1);
        }
        
        // Create client
        try (GroqApiClient client = GroqClientFactory.createClient(API_KEY)) {
            // Example 1: Simple chat completion
            simpleChatExample(client);
            
            // Example 2: Stream chat completion
            streamChatExample(client);
            
            // Example 3: Tools example
            toolsExample(client);
            
            // Example 4: Vision example
            visionExample(client);
        }
    }
    
    /**
     * Example of a simple chat completion.
     */
    private static void simpleChatExample(GroqApiClient client) throws Exception {
        System.out.println("\n=== Simple Chat Example ===");
        
        String model = "deepseek-r1-distill-llama-70b";
        String userMessage = "What are the benefits of using Java records?";
        String systemMessage = "You are a helpful programming assistant.";
        
        // Using extensions for simpler API
        String response = GroqApiExtensions.chatText(
            client, model, userMessage, systemMessage).get(30, TimeUnit.SECONDS);
            
        System.out.println("Response: " + response);
    }
    
    /**
     * Example of a streaming chat completion.
     */
    private static void streamChatExample(GroqApiClient client) throws Exception {
        System.out.println("\n=== Streaming Chat Example ===");
        
        String model = "deepseek-r1-distill-llama-70b";
        String userMessage = "Count from 1 to 10";
        
        // Subscribe to the stream
        CompletableFuture<Void> completion = new CompletableFuture<>();
        
        GroqApiExtensions.chatTextStream(client, model, userMessage, null)
            .subscribe(new SimpleSubscriber<>(
                text -> System.out.print(text),
                throwable -> {
                    System.err.println("Error: " + throwable.getMessage());
                    completion.completeExceptionally(throwable);
                },
                () -> {
                    System.out.println("\nStream completed");
                    completion.complete(null);
                }
            ));
        
        // Wait for completion
        completion.get(30, TimeUnit.SECONDS);
    }
    
    /**
     * Example of using tools.
     */
    private static void toolsExample(GroqApiClient client) throws Exception {
        System.out.println("\n=== Tools Example ===");
        
        // Define a simple weather tool
        ObjectNode weatherParams = MAPPER.createObjectNode();
        
        // Set the root-level type to "object"
        weatherParams.put("type", "object");
        
        // Define properties
        ObjectNode properties = MAPPER.createObjectNode();
        ObjectNode locationProp = MAPPER.createObjectNode();
        locationProp.put("type", "string");
        locationProp.put("description", "The city and state, e.g. San Francisco, CA");
        properties.set("location", locationProp);
        weatherParams.set("properties", properties);
        
        // Define required fields
        ArrayNode requiredArray = MAPPER.createArrayNode();
        requiredArray.add("location");
        weatherParams.set("required", requiredArray);
        
        Function weatherFunction = new Function(
            "get_current_weather",
            "Get the current weather in a given location",
            weatherParams,
            args -> {
                try {
                    // Parse location from arguments
                    JsonNode argsNode = MAPPER.readTree(args);
                    String location = argsNode.path("location").asText("Unknown");
                    
                    // Simulate weather data
                    ObjectNode weatherData = MAPPER.createObjectNode()
                        .put("location", location)
                        .put("temperature", 72)
                        .put("unit", "fahrenheit")
                        .put("description", "Sunny");
                    
                    return CompletableFuture.completedFuture(weatherData.toString());
                } catch (JsonProcessingException e) {
                    // Return a future completeExceptionally 
                    CompletableFuture<String> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                }
            }
        );
        
        Tool weatherTool = Tool.functionTool(weatherFunction);
        
        // Run conversation with the tool
        String response = client.runConversationWithTools(
            "What's the weather like in San Francisco?",
            List.of(weatherTool),
            "meta-llama/llama-4-maverick-17b-128e-instruct",
            "You are a helpful assistant with access to weather information."
        ).get(30, TimeUnit.SECONDS);
        
        System.out.println("Response: " + response);
    }
    
    /**
     * Example of using vision capabilities.
     */
    private static void visionExample(GroqApiClient client) throws Exception {
        System.out.println("\n=== Vision Example ===");
        
        // For demonstration, we'll use a public image URL
        // In a real application, you might have a local image file
        String imageUrl = "https://images.unsplash.com/photo-1557428894-56bcc97113fe?q=80&w=1000";
        String prompt = "Describe this image in detail.";
        
        JsonNode response = client.createVisionCompletionWithImageUrl(
            imageUrl,
            prompt,
            "llama-3.2-90b-vision-preview",
            0.7f
        ).get(30, TimeUnit.SECONDS);
        
        String content = response
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText("No description available");
            
        System.out.println("Image description: " + content);
        
        // Example of using a local image (commented out)
        /*
        String imagePath = "path/to/local/image.jpg";
        JsonNode localImageResponse = client.createVisionCompletionWithBase64Image(
            imagePath,
            prompt,
            "llama-3.2-90b-vision-preview",
            0.7f
        ).get(30, TimeUnit.SECONDS);
        
        String localImageContent = JsonUtils.extractContentFromCompletion(localImageResponse);
        System.out.println("Local image description: " + localImageContent);
        */
    }
    
    /**
     * Simple implementation of a Flow.Subscriber for handling streams.
     */
    private static class SimpleSubscriber<T> implements Flow.Subscriber<T> {
        private final Consumer<T> onNext;
        private final Consumer<Throwable> onError;
        private final Runnable onComplete;
        private Flow.Subscription subscription;
        
        public SimpleSubscriber(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE); // Request all items
        }
        
        @Override
        public void onNext(T item) {
            onNext.accept(item);
        }
        
        @Override
        public void onError(Throwable throwable) {
            onError.accept(throwable);
        }
        
        @Override
        public void onComplete() {
            onComplete.run();
        }
    }
}