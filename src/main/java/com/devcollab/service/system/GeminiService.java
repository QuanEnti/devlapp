package com.devcollab.service.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GeminiService {

    private static final String API_KEY = "AIzaSyCZGenpihGSGq_FOnVU7GG7KhHvvna2eqs";
    private static final String MODEL = "gemini-2.0-flash";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private final ObjectMapper mapper = new ObjectMapper();

    public String getGeminiResponse(String prompt) {
        try {
            // Build request body
            String json = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": "%s" }
                      ]
                    }
                  ]
                }
                """.formatted(prompt.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return parseResponse(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Error calling Gemini API: " + e.getMessage();
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.asText().isEmpty()) {
                return "⚠️ Gemini returned no text: " + responseBody;
            }

            return textNode.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Unable to parse Gemini response: " + e.getMessage() +
                    "\nRaw response: " + responseBody;
        }
    }
}
