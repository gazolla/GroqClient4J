package com.groq.api.client;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service interface for audio operations.
 */
public interface AudioService {
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
    CompletableFuture<JsonNode> createTranscription(
        InputStream audioFile,
        String fileName,
        String model,
        String prompt,
        String responseFormat,
        String language,
        Float temperature
    );
    
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
    CompletableFuture<JsonNode> createTranslation(
        InputStream audioFile,
        String fileName,
        String model,
        String prompt,
        String responseFormat,
        Float temperature
    );
}