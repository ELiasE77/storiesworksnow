package com.digitallife.journal_site.Journal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Analyses an uploaded image and extracts a concise description of the scene
 * so we can reuse the scenery cues in later AI generations.
 */
@Service
public class ImageDescriptionService {

    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<String> describeScene(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            return Optional.empty();
        }
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            return Optional.empty();
        }

        try {
            JSONArray messages = new JSONArray();

            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You describe the scenery in an image in 2-3 vivid sentences. "
                            + "Focus on location, lighting, weather, colors, and notable objects. "
                            + "Do not mention people or create fictional details."));

            JSONArray userContent = new JSONArray();
            userContent.put(new JSONObject()
                    .put("type", "text")
                    .put("text", "Summarize the visible setting and mood in 2-3 concise sentences."));

            JSONObject imageUrl = new JSONObject()
                    .put("url", "data:image/png;base64," + base64Image)
                    .put("detail", "low");

            userContent.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", imageUrl));

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userContent));

            JSONObject body = new JSONObject()
                    .put("model", "gpt-4o-mini")
                    .put("messages", messages)
                    .put("max_tokens", 200)
                    .put("temperature", 0.2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(OPENAI_API_KEY);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(CHAT_URL, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Optional.empty();
            }

            JSONObject json = new JSONObject(response.getBody());
            String description = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

            return description.isBlank() ? Optional.empty() : Optional.of(description);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}