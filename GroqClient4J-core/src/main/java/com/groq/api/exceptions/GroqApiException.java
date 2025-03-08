package com.groq.api.exceptions;

/**
 * Exception thrown when an error occurs during API operations.
 */
public class GroqApiException extends Exception {
	private static final long serialVersionUID = 1L;
	private final int statusCode;
    private final String errorType;
    private final String errorCode;

    /**
     * Creates a new exception with status code and message.
     *
     * @param statusCode The HTTP status code or error code.
     * @param message A descriptive error message.
     */
    public GroqApiException(int statusCode, String message) {
        this(statusCode, message, null, null, null);
    }

    /**
     * Creates a new exception with status code, message, and cause.
     *
     * @param statusCode The HTTP status code or error code.
     * @param message A descriptive error message.
     * @param cause The underlying cause of the exception.
     */
    public GroqApiException(int statusCode, String message, Throwable cause) {
        this(statusCode, message, null, null, cause);
    }

    /**
     * Creates a new exception with all details.
     *
     * @param statusCode The HTTP status code or error code.
     * @param message A descriptive error message.
     * @param errorType The type of error returned by the API.
     * @param errorCode The error code returned by the API.
     * @param cause The underlying cause of the exception.
     */
    public GroqApiException(
            int statusCode,
            String message,
            String errorType,
            String errorCode,
            Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    /**
     * Gets the status code associated with this exception.
     *
     * @return The HTTP status code or error code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the error type if available.
     *
     * @return The error type or null.
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Gets the error code if available.
     *
     * @return The error code or null.
     */
    public String getErrorCode() {
        return errorCode;
    }
}