# GroqClient4J

A modern Java client library for interacting with the [Groq API](https://console.groq.com/docs/api), enabling seamless integration of Groq's powerful large language models in Java applications.

## Features

- Complete Java implementation for the Groq API
- Asynchronous operation using Java's CompletableFuture and Flow API
- Support for all Groq API capabilities:
  - Chat completions (with streaming support)
  - Vision models
  - Tool/function calling
  - Audio transcription and translation
  - Model management
- Comprehensive error handling
- Easy-to-use extension methods for common operations

## Requirements

- Java 17 or higher
- Jackson for JSON processing
- Maven or Gradle build tool

## Installation

### Maven

```xml
<dependency>
    <groupId>com.groq</groupId>
    <artifactId>groqclient4j-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.groq:groqclient4j-core:1.0.0-SNAPSHOT'
```

## Quick Start

```java
import com.groq.api.client.GroqApiClient;
import com.groq.api.client.GroqClientFactory;
import com.groq.api.extensions.GroqApiExtensions;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Get API key from environment variable
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Please set the GROQ_API_KEY environment variable");
        }
        
        // Create client
        try (GroqApiClient client = GroqClientFactory.createClient(apiKey)) {
            // Simple chat example
            String response = GroqApiExtensions.chatText(
                client,
                "llama-3.2-70b",
                "What are the benefits of using Java records?",
                "You are a helpful programming assistant."
            ).get();
            
            System.out.println("Response: " + response);
        }
    }
}
```

## Examples

### Simple Chat

```java
String response = GroqApiExtensions.chatText(
    client,
    "llama-3.2-70b",
    "Explain Java CompletableFuture in simple terms",
    null
).get(30, TimeUnit.SECONDS);
```

### Streaming Response

```java
CompletableFuture<Void> completion = new CompletableFuture<>();

GroqApiExtensions.chatTextStream(client, "llama-3.2-70b", "Count from 1 to 10", null)
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
```

### Function/Tool Calling

```java
// Define a weather tool
ObjectNode weatherParams = MAPPER.createObjectNode();
// ... parameter configuration

Function weatherFunction = new Function(
    "get_current_weather",
    "Get the current weather in a given location",
    weatherParams,
    args -> {
        // Call actual weather API
        return fetchWeatherData(args);
    }
);

Tool weatherTool = Tool.functionTool(weatherFunction);

// Run conversation with the tool
String response = client.runConversationWithTools(
    "What's the weather like in Miami? Should I bring an umbrella?",
    List.of(weatherTool),
    "llama-3.2-70b",
    "You are a helpful assistant with access to weather information."
).get(30, TimeUnit.SECONDS);
```

### Vision API

```java
String imageUrl = "https://example.com/image.jpg";
String prompt = "Describe this image in detail.";

JsonNode response = client.createVisionCompletionWithImageUrl(
    imageUrl,
    prompt,
    "llama-3.2-90b-vision-preview",
    0.7f
).get(30, TimeUnit.SECONDS);
```

## Project Structure

```
com.groq.api/
├── client/
│   ├── AudioService.java
│   ├── ChatCompletionService.java
│   ├── GroqApi.java
│   ├── GroqApiClient.java
│   ├── GroqClientFactory.java
│   ├── ModelsService.java
│   ├── ToolsService.java
│   └── VisionService.java
├── config/
│   └── GroqApiConfig.java
├── exceptions/
│   └── GroqApiException.java
├── extensions/
│   └── GroqApiExtensions.java
├── models/
│   ├── Function.java
│   └── Tool.java
└── utils/
    ├── ImageUtils.java
    └── JsonUtils.java
```

## Error Handling

The library uses `GroqApiException` to provide clear error information. All asynchronous methods return `CompletableFuture` that may complete exceptionally with a `GroqApiException`.

```java
client.createChatCompletion(request)
    .thenAccept(response -> System.out.println("Success: " + response))
    .exceptionally(e -> {
        if (e.getCause() instanceof GroqApiException) {
            GroqApiException apiError = (GroqApiException) e.getCause();
            System.err.println("API Error: " + apiError.getMessage() + 
                ", Status: " + apiError.getStatusCode());
        }
        return null;
    });
```

## Advanced Configuration

You can customize the HTTP client and configure timeouts:

```java
// Create custom HTTP client
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();

// Create custom object mapper
ObjectMapper mapper = new ObjectMapper();

// Create client with custom configuration
GroqApiConfig config = new GroqApiConfig(
    apiKey,
    GroqApiConfig.DEFAULT_BASE_URL,
    GroqApiConfig.MAX_BASE64_SIZE_MB
);

GroqApiClient client = GroqClientFactory.createClient(config, httpClient, mapper);
```

## Building from Source

```bash
git clone https://github.com/yourusername/groqclient4j.git
cd groqclient4j
mvn clean install
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[MIT License](LICENSE)