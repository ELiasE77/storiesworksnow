package com.digitallife.journal_site.ChatGptIntegration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Map;

/**
 * Handles AI-powered features:
 * - Journal feedback from fine-tuned GPT model
 * - Image generation with DALL·E 3
 */
@RestController
@RequestMapping("/api")
public class ChatGPTController {

    // Load API key from environment (set in /etc/environment or systemd service)
    private final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    private static final String FINE_TUNED_MODEL_ID =
            "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";

    /**
     * Endpoint: POST /api/get-feedback
     * Expects JSON like: { "content": "journal text here" }
     * Returns: { "feedback": "..." }
     */
    @PostMapping(
            value = "/get-feedback",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> getFeedback(@RequestBody Map<String, String> request) throws JSONException {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"OPENAI_KEY environment variable is not set\"}");
        }

        String journalEntry = request.get("content");
        if (journalEntry == null || journalEntry.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Journal entry content missing\"}");
        }

        String url = "https://api.openai.com/v1/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        // Build messages for fine-tuned GPT
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content",
                        "You are an assistant that gives constructive feedback on journal entries. " +
                                "Your feedback should always be between 2 and 7 sentences, " +
                                "concise yet helpful, and never longer than 200 words."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Give feedback on the following journal entry:\n\n" + journalEntry));

        // Request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", FINE_TUNED_MODEL_ID);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);  // bumped up
        requestBody.put("temperature", 0.5);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OPENAI_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        // Call OpenAI API
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ResponseEntity.status(response.getStatusCode())
                    .body("{\"error\":\"OpenAI returned no body\"}");
        }

        // Parse feedback text out of OpenAI response
        JSONObject openaiResponse = new JSONObject(response.getBody());
        String feedback = openaiResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Return clean JSON
        JSONObject out = new JSONObject().put("feedback", feedback);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(out.toString());
    }

    /**
     * Endpoint: POST /api/generate-image
     * Expects JSON like: {
     *   "journalText": "some text",
     *   "style": "sketch",
     *   "pictureType": "regular|personalized|none",
     *   "persona": "optional persona info"
     * }
     * Returns: { "base64Image": "..." }
     */
    @PostMapping(
            value = "/generate-image",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> generateImage(@RequestBody Map<String, String> request) throws JSONException {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"OPENAI_KEY environment variable is not set\"}");
        }

        String journalText = request.get("journalText");
        String style = request.getOrDefault("style", "regular");
        String pictureType = request.getOrDefault("pictureType", "regular");
        String persona = request.getOrDefault("persona", "");

        if ("none".equalsIgnoreCase(pictureType)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"base64Image\":\"\"}");
        }

        String prompt;
        if ("personalized".equalsIgnoreCase(pictureType)) {
            prompt = "Create a " + style + " image based on the user's persona (" + persona + ") "
                    + "and the journal text. Focus on a positive moment:\n\n" + journalText;
        } else {
            prompt = "Create a " + style + " image of one positive moment from the following journal. "
                    + "Focus on location and objects only, no people:\n\n" + journalText;
        }

        String url = "https://api.openai.com/v1/images/generations";
        RestTemplate restTemplate = new RestTemplate();

        // Request body for DALL·E
        JSONObject body = new JSONObject();
        body.put("model", "dall-e-3");
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        body.put("quality", "standard");

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OPENAI_API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // Extract image URL
        String imageUrl;
        if (response.getStatusCode().is2xxSuccessful()) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            imageUrl = jsonResponse.getJSONArray("data").getJSONObject(0).getString("url");
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }

        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"base64Image\":\"" + base64Image + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to fetch and encode image: " + e.getMessage() + "\"}");
        }
    }
}
