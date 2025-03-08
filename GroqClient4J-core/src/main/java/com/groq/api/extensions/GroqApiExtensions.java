package com.groq.api.extensions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.groq.api.client.GroqApiClient;
import com.groq.api.utils.JsonUtils;

/**
 * Extension methods to simplify common operations with the Groq API client.
 */
public final class GroqApiExtensions {
    private GroqApiExtensions() {
        // Utility class should not be instantiated
    }

    /**
     * Creates a chat completion and returns the request object.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @param temperature The temperature parameter for generation.
     * @return A CompletableFuture that will complete with the JSON response.
     */
    public static CompletableFuture<JsonNode> chat(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage,
            float temperature) {
        
        JsonNode request = JsonUtils.createSimpleChatRequest(
            model,
            userMessage,
            systemMessage,
            temperature
        );
        
        return client.createChatCompletion(request);
    }

    /**
     * Creates a chat completion and returns the request object with default temperature.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @return A CompletableFuture that will complete with the JSON response.
     */
    public static CompletableFuture<JsonNode> chat(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage) {
        
        return chat(client, model, userMessage, systemMessage, 0.7f);
    }

    /**
     * Creates a chat completion and returns just the text content.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @param temperature The temperature parameter for generation.
     * @return A CompletableFuture that will complete with the text content.
     */
    public static CompletableFuture<String> chatText(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage,
            float temperature) {
        
        return chat(client, model, userMessage, systemMessage, temperature)
            .thenApply(response -> {
                String content = JsonUtils.extractContentFromCompletion(response);
                return content != null ? content : "";
            });
    }

    /**
     * Creates a chat completion and returns just the text content with default temperature.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @return A CompletableFuture that will complete with the text content.
     */
    public static CompletableFuture<String> chatText(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage) {
        
        return chatText(client, model, userMessage, systemMessage, 0.7f);
    }

    /**
     * Creates a streaming chat completion.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @param temperature The temperature parameter for generation.
     * @return A publisher that will emit JSON objects as they arrive.
     */
    public static Flow.Publisher<JsonNode> chatStream(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage,
            float temperature) {
        
        JsonNode request = JsonUtils.createSimpleChatRequest(
            model,
            userMessage,
            systemMessage,
            temperature
        );
        
        return client.createChatCompletionStream(request);
    }

    /**
     * Creates a streaming chat completion with default temperature.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @return A publisher that will emit JSON objects as they arrive.
     */
    public static Flow.Publisher<JsonNode> chatStream(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage) {
        
        return chatStream(client, model, userMessage, systemMessage, 0.7f);
    }

    /**
     * Creates a streaming chat completion and returns just the text content.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @param temperature The temperature parameter for generation.
     * @return A publisher that will emit text fragments as they arrive.
     */
    public static Flow.Publisher<String> chatTextStream(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage,
            float temperature) {
        
        Flow.Publisher<JsonNode> jsonPublisher = chatStream(
            client, model, userMessage, systemMessage, temperature);
        
        // Transform JsonNode stream to String stream
        return new JsonToStringPublisher(jsonPublisher, JsonUtils::extractContentFromChunk);
    }

    /**
     * Creates a streaming chat completion with default temperature and returns just the text content.
     *
     * @param client The Groq API client.
     * @param model The model to use.
     * @param userMessage The user's message.
     * @param systemMessage Optional system message.
     * @return A publisher that will emit text fragments as they arrive.
     */
    public static Flow.Publisher<String> chatTextStream(
            GroqApiClient client,
            String model,
            String userMessage,
            String systemMessage) {
        
        return chatTextStream(client, model, userMessage, systemMessage, 0.7f);
    }

    /**
     * A processor that transforms a Flow.Publisher<JsonNode> to a Flow.Publisher<String>
     * by applying a mapping function to each JsonNode.
     */
    private static class JsonToStringPublisher implements Flow.Publisher<String> {
        private final Flow.Publisher<JsonNode> source;
        private final Function<JsonNode, String> mapper;
        
        public JsonToStringPublisher(Flow.Publisher<JsonNode> source, Function<JsonNode, String> mapper) {
            this.source = source;
            this.mapper = mapper;
        }
        
        @Override
        public void subscribe(Flow.Subscriber<? super String> subscriber) {
            source.subscribe(new MapperSubscriber(subscriber, mapper));
        }
        
        private static class MapperSubscriber implements Flow.Subscriber<JsonNode> {
            private final Flow.Subscriber<? super String> downstream;
            private final Function<JsonNode, String> mapper;
            private Flow.Subscription subscription;
            
            public MapperSubscriber(Flow.Subscriber<? super String> downstream, Function<JsonNode, String> mapper) {
                this.downstream = downstream;
                this.mapper = mapper;
            }
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                downstream.onSubscribe(subscription);
            }
            
            @Override
            public void onNext(JsonNode item) {
                String mapped = mapper.apply(item);
                if (mapped != null && !mapped.isEmpty()) {
                    downstream.onNext(mapped);
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                downstream.onError(throwable);
            }
            
            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}