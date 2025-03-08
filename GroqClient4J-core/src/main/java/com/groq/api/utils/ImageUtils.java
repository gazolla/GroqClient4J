package com.groq.api.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.groq.api.config.GroqApiConfig;
import com.groq.api.exceptions.GroqApiException;

/**
 * Utility class for working with images in the Groq API.
 */
public final class ImageUtils {
    
    private ImageUtils() {
        // Utility class should not be instantiated
    }
    
    /**
     * Converts an image file to a Base64-encoded string.
     *
     * @param imagePath The path to the image file.
     * @return A Base64-encoded string representing the image.
     * @throws NoSuchFileException If the image file does not exist.
     * @throws IOException If there's an error reading the file.
     */
    public static String convertImageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        if (!file.exists()) {
            throw new NoSuchFileException("Image file not found: " + imagePath);
        }

        byte[] fileBytes = Files.readAllBytes(Path.of(imagePath));
        return Base64.getEncoder().encodeToString(fileBytes);
    }
    
    /**
     * Validates that the model in the request is a valid vision model.
     *
     * @param request The JSON request object.
     * @throws GroqApiException If the model is invalid.
     */
    public static void validateVisionModel(JsonNode request) throws GroqApiException {
        JsonNode modelNode = request.path("model");
        if (modelNode.isMissingNode() || !modelNode.isTextual()) {
            throw new GroqApiException(
                400,
                "Invalid vision model. Must be one of: " + String.join(", ", GroqApiConfig.VISION_MODELS)
            );
        }
        
        String model = modelNode.asText();
        if (!GroqApiConfig.VISION_MODELS.contains(model)) {
            throw new GroqApiException(
                400,
                "Invalid vision model. Must be one of: " + String.join(", ", GroqApiConfig.VISION_MODELS)
            );
        }
    }
    
    /**
     * Validates that a Base64-encoded image does not exceed the maximum size.
     *
     * @param base64String The Base64-encoded image.
     * @param maxSizeMB The maximum size in megabytes.
     * @throws GroqApiException If the image exceeds the maximum size.
     */
    public static void validateBase64Size(String base64String, int maxSizeMB) throws GroqApiException {
        double sizeInMB = (base64String.length() * 3.0 / 4.0) / (1024 * 1024);
        if (sizeInMB > maxSizeMB) {
            throw new GroqApiException(
                400,
                "Base64 image exceeds the maximum size of " + maxSizeMB + " MB"
            );
        }
    }
    
    /**
     * Validates that an image URL is in a valid format.
     *
     * @param url The image URL to validate.
     * @throws GroqApiException If the URL is invalid.
     */
    public static void validateImageUrl(String url) throws GroqApiException {
        if (url == null || url.isBlank()) {
            throw new GroqApiException(
                400,
                "Image URL cannot be empty"
            );
        }

        try {
            new URL(url);
        } catch (Exception e) {
            throw new GroqApiException(
                400,
                "Invalid image URL format",
                e
            );
        }
    }
}