package com.groq.api.examples;

import com.groq.api.client.GroqApiClient;
import com.groq.api.client.GroqClientFactory;
import com.groq.api.extensions.GroqApiExtensions;

public class SimpleChatExample {
    public static void main(String[] args) throws Exception {
        
    	 String apiKey = "";
    	 
        if (apiKey.isEmpty()) {
            System.out.println("Missing API_KEY");
            System.exit(1);
        }
       
        try (GroqApiClient client = GroqClientFactory.createClient(apiKey)) {
           
            String response = GroqApiExtensions.chatText(
                client,
                "llama-3.3-70b-versatile",
                "Explain Assicronous programming in Java",
                "You are a Java expert."
            ).get();
            
            System.out.println("\nResposta:");
            System.out.println(response);
        }
    }
}