package com.digitallife.journal_site.profile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PersonaService {

    private static final String OPENAI_API_KEY =
            "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";
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate rest = new RestTemplate();

    /**
     * Generates a 300–500 word persona feature based on the saved questionnaire.
     */
    public String generatePersonaFeature(Profile profile) {
        try {
            // 1) build the chat messages
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are an expert storyteller who writes vivid personas."));
            String userPrompt = String.format("""
                Write a 300–500 word persona feature about a %d-year-old %s with height %s, nationality %s, hair %s, who enjoys %s, and whose personal outlook is described as: %s. Include their social surroundings and context.
                """,
                    profile.getAge(),
                    profile.getGender(),
                    profile.getHeight(),
                    profile.getNationality(),
                    profile.getHair(),
                    profile.getHobbies(),
                    profile.getPersona()
            );
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userPrompt)
            );

            // 2) build request body
            JSONObject body = new JSONObject();
            body.put("model", "gpt-4o-mini");
            body.put("messages", messages);
            body.put("max_tokens", 600);
            body.put("temperature", 0.7);

            // 3) set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(OPENAI_API_KEY);

            HttpEntity<String> req = new HttpEntity<>(body.toString(), headers);

            // 4) call OpenAI
            ResponseEntity<String> resp = rest.postForEntity(CHAT_URL, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "OpenAI API error " + resp.getStatusCode().value() + ": " + resp.getBody()
                );
            }

            // 5) parse out the generated text
            JSONObject json = new JSONObject(resp.getBody());
            return json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate persona feature", e);
        }
    }
}
