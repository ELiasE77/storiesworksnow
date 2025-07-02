package com.digitallife.journal_site.profile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service that communicates with OpenAI to generate and update
 * the long-form persona feature text.
 */
@Service
public class PersonaService {

    private static final String OPENAI_API_KEY =
            "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";
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate rest = new RestTemplate();

    /**
     * Analyse the uploaded image and return a JSON description. The JSON is
     * also written to a <code>.jsonl</code> file next to the uploaded picture
     * so the data can be reused for image generation.
     */
    public String analyseImage(java.nio.file.Path imgPath, Long userId) {
        try {
            long size = java.nio.file.Files.size(imgPath);
            String json = new JSONObject().put("fileSize", size).toString();

            java.nio.file.Path jsonPath = imgPath.getParent()
                    .resolve("profile_" + userId + ".jsonl");
            java.nio.file.Files.writeString(jsonPath, json + "\n");
            return json;
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Generates a persona feature from the questionnaire answers.
     * The summary should start short (around 10 words) but may grow up to
     * about 500 words as more journal entries are added. Only the
     * supplied details may be used—no invented facts such as residence or
     * names.
     */
    public String generatePersonaFeature(Profile profile) {
        try {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Summarise the given details into a concise persona feature between 10 and 500 words. Do not invent any information."));
            String prompt = String.format("""
%s is a %d-year-old %s, height %s, nationality %s, hair %s, hobbies %s. Outlook: %s
""",
                    profile.getName(),
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
                    .put("content", prompt));

            JSONObject body = new JSONObject();
            body.put("model", "gpt-4o-mini");
            body.put("messages", messages);
            body.put("max_tokens", 750);
            body.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(OPENAI_API_KEY);

            HttpEntity<String> req = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> resp = rest.postForEntity(CHAT_URL, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("OpenAI API error: " + resp.getBody());
            }

            JSONObject json = new JSONObject(resp.getBody());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate persona feature", e);
        }
    }

    /**
     * Update an existing persona feature with a new journal entry.
     */
    public String updatePersonaFeature(Profile profile, String entryContent) {
        try {
            String current = profile.getPersonaFeature();
            if (current == null || current.isBlank()) {
                current = generatePersonaFeature(profile);
            }

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Update the persona feature between 10 and 500 words total. Incorporate factual information from the new entry while gradually expanding the summary."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "Current summary:" + current + "\nNew entry:" + entryContent));

            JSONObject body = new JSONObject();
            body.put("model", "gpt-4o-mini");
            body.put("messages", messages);
            body.put("max_tokens", 750);
            body.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(OPENAI_API_KEY);

            HttpEntity<String> req = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> resp = rest.postForEntity(CHAT_URL, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("OpenAI API error: " + resp.getBody());
            }

            JSONObject json = new JSONObject(resp.getBody());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update persona feature", e);
        }
    }
}
