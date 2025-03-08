package com.groq.api.client;

/**
 * Main interface for the Groq API, combining all service interfaces.
 */
public interface GroqApi extends 
    ChatCompletionService, 
    AudioService, 
    VisionService, 
    ToolsService, 
    ModelsService, 
    AutoCloseable {
}