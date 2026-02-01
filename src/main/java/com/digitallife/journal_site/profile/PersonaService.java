package com.digitallife.journal_site.profile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service that communicates with OpenAI to generate and update
 * the long-form persona feature text.
 */
@Service
public class PersonaService {
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private final RestTemplate rest = new RestTemplate();

    /**
     * Analyse the uploaded image and return a JSON description.
     * Also writes a `.jsonl` file next to the uploaded picture.
     */
    public String analyseImage(Path imgPath, Long userId) {
        try {
            long size = Files.size(imgPath);
            String json = new JSONObject().put("fileSize", size).toString();

            Path jsonPath = imgPath.getParent().resolve("profile_" + userId + ".jsonl");
            Files.writeString(jsonPath, json + "\n");

            return json;
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Generates a persona feature from the questionnaire answers.
     * Starts short (10–500 words), grows up to ~500 words with journal entries.
     */
    public String generatePersonaFeature(Profile profile) {
        try {
            ensureApiKey();
            JSONArray messages = new JSONArray();

            // System prompt
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "You are a biographer crafting a warm, insightful third-person essay of ~500 words. "
                                    + "Use only the provided details, do not invent any information."));

            // User prompt
            String prompt = String.format("""
                            %s is a %d-year-old %s, height %s, nationality %s, hair %s, hobbies %s.
                            Outlook: %s

                            Base profile information:
                            %s

                            Journal background / personal outlook:
                            %s

                            Task: Write an engaging third-person persona essay of 480–520 words that summarises who this person is. "
                            "Highlight key personality traits, motivations, and themes from the supplied details without inventing facts.
                            """,
                    profile.getName(),
                    profile.getAge(),
                    profile.getGender(),
                    profile.getHeight(),
                    profile.getNationality(),
                    profile.getHair(),
                    profile.getHobbies(),
                    profile.getPersona(),
                    buildProfileOverview(profile),
                    nullToEmpty(profile.getPersona())
            );

            messages.put(new JSONObject().put("role", "user").put("content", prompt));

            JSONObject body = new JSONObject()
                    .put("model", "gpt-4o-mini")
                    .put("messages", messages)
                    .put("max_tokens", 1500)
                    .put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

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
            ensureApiKey();
            String current = profile.getPersonaFeature();
            if (current == null || current.isBlank()) {
                current = generatePersonaFeature(profile);
            }

            JSONArray messages = new JSONArray();

            // System prompt
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "You maintain an evolving biography. "
                                    + "Refresh it to roughly 500 words by weaving in the latest entry "
                                    + "while preserving truthful details from the existing summary."));

            // User prompt
            String prompt = String.format("""
                            Base profile information:
                            %s

                            Existing persona essay:
                            %s

                            Latest journal entry to incorporate:
                            %s

                            Task: Produce an updated third-person persona essay of 480–520 words. "
                            "Preserve important facts from the existing essay and add any new insights from the latest entry. "
                            "Avoid repetition and do not invent details beyond what has been provided.
                            """,
                    buildProfileOverview(profile),
                    current,
                    nullToEmpty(entryContent)
            );

            messages.put(new JSONObject().put("role", "user").put("content", prompt));

            JSONObject body = new JSONObject()
                    .put("model", "gpt-4o-mini")
                    .put("messages", messages)
                    .put("max_tokens", 1500)
                    .put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

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

    private String buildProfileOverview(Profile profile) {
        StringBuilder sb = new StringBuilder();
        if (profile.getName() != null) sb.append("Name: ").append(profile.getName()).append('\n');
        if (profile.getAge() != null) sb.append("Age: ").append(profile.getAge()).append('\n');
        if (profile.getGender() != null) sb.append("Gender: ").append(profile.getGender()).append('\n');
        if (profile.getHeight() != null) sb.append("Height: ").append(profile.getHeight()).append('\n');
        if (profile.getNationality() != null) sb.append("Nationality: ").append(profile.getNationality()).append('\n');
        if (profile.getHair() != null) sb.append("Hair: ").append(profile.getHair()).append('\n');
        if (profile.getHobbies() != null) sb.append("Hobbies: ").append(profile.getHobbies()).append('\n');
        return sb.toString().trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void ensureApiKey() {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not set");
        }
    }
}
