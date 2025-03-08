package com.groq.api.models;

/**
 * Represents a tool that can be used by the Groq API.
 */
public record Tool(
    String type,
    Function function
) {
    /**
     * Creates a new function tool with the default type.
     *
     * @param function The function for this tool.
     * @return A new Tool instance.
     */
    public static Tool functionTool(Function function) {
        return new Tool("function", function);
    }
}