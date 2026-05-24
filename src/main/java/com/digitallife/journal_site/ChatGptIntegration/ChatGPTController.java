package com.digitallife.journal_site.ChatGptIntegration;

import com.digitallife.journal_site.common.SiteTextService;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.profile.VisualMemoryService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles AI-powered features:
 * - Journal feedback plus segmented reflection prompts
 * - Image generation with Journally style presets
 */
@RestController
@RequestMapping("/api")
public class ChatGPTController {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String IMAGE_URL = "https://api.openai.com/v1/images/generations";
    private static final int MAX_IMAGE_PROMPT_LENGTH = 3900;
    private static final int MAX_JOURNAL_CONTEXT_LENGTH = 1600;
    private static final int MAX_PERSONA_CONTEXT_LENGTH = 180;
    private static final int MAX_VISUAL_MEMORY_CONTEXT_LENGTH = 320;
    private static final int VISUAL_MEMORY_QUESTION_INDEX = 2;
    private static final String FINE_TUNED_MODEL_ID =
            "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";
    private static final String PROMPT_BUILDER_MODEL_ID = "gpt-4o-mini";

    private final OpenAiKeyProvider openAiKeyProvider;
    private final SiteTextService siteTextService;
    private final ProfileRepository profileRepository;
    private final VisualMemoryService visualMemoryService;
    private final RestTemplate restTemplate = new RestTemplate();

    public ChatGPTController(
            OpenAiKeyProvider openAiKeyProvider,
            SiteTextService siteTextService,
            ProfileRepository profileRepository,
            VisualMemoryService visualMemoryService
    ) {
        this.openAiKeyProvider = openAiKeyProvider;
        this.siteTextService = siteTextService;
        this.profileRepository = profileRepository;
        this.visualMemoryService = visualMemoryService;
    }

    @PostMapping(
            value = "/get-feedback",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> getFeedback(@RequestBody Map<String, String> request) throws JSONException {
        Locale locale = LocaleContextHolder.getLocale();
        if (!openAiKeyProvider.isConfigured()) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, openAiKeyProvider.getConfigurationHelp());
        }

        String journalEntry = request.get("content");
        if (journalEntry == null || journalEntry.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, siteTextService.message("ai.error.feedbackMissingContent", locale));
        }

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content",
                        """
                        You are Journally's reflection companion.
                        Respond with valid JSON only in this exact shape:
                        {
                          "feedback": "2-4 warm, concise sentences of reflective feedback",
                          "sentimentScore": 4,
                          "questions": [
                            "Question 1",
                            "Question 2",
                            "Question 3"
                          ],
                          "visualMemoryQuestionIndex": 2
                        }
                        Question 1 should focus on the user's emotional experience.
                        Question 2 should focus on the setting, atmosphere, or surrounding details.
                        Question 3 must ask the user to describe one person, place, or concrete detail they mentioned or implied, so the answer can later help image generation.
                        Keep each question short enough to answer in a form field.
                        sentimentScore must be an integer from 1 to 7 where 1 = very heavy/distressed and 7 = very light/energized.
                        Write the feedback and all questions in %s.
                        """
                                .formatted(languageNameForPrompt(locale))
                ));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Create Journally feedback for this reflection:\n\n" + journalEntry));

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", FINE_TUNED_MODEL_ID);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 450);
        requestBody.put("temperature", 0.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiKeyProvider.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                    CHAT_URL,
                    new HttpEntity<>(requestBody.toString(), headers),
                    String.class
            );
        } catch (HttpStatusCodeException e) {
            return openAiErrorResponse(e);
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    siteTextService.message("ai.error.feedbackRequestFailed", locale, e.getMessage()));
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return errorResponse(response.getStatusCode(), siteTextService.message("ai.error.emptyResponse", locale));
        }

        try {
            JSONObject openaiResponse = new JSONObject(response.getBody());
            String assistantContent = openaiResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

            JSONObject parsed = parseStructuredFeedback(assistantContent, journalEntry, locale);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(parsed.toString());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, siteTextService.message("ai.error.feedbackParse", locale));
        }
    }

    @PostMapping(
            value = "/generate-image",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> generateImage(@RequestBody Map<String, Object> request, Authentication auth) throws JSONException {
        Locale locale = LocaleContextHolder.getLocale();
        if (!openAiKeyProvider.isConfigured()) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, openAiKeyProvider.getConfigurationHelp());
        }

        String journalText = asString(request.get("journalText"));
        String style = asString(request.get("style"));
        String persona = asString(request.get("persona"));
        String visualMemoryAnswer = asString(request.get("visualMemoryAnswer"));
        List<String> contextImages = asStringList(request.get("contextImages"));

        if (journalText == null || journalText.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, siteTextService.message("ai.error.imageMissingContent", locale));
        }

        Profile profile = resolveProfile(auth);
        String resolvedPersona = profile != null && profile.getPersonaFeature() != null && !profile.getPersonaFeature().isBlank()
                ? profile.getPersonaFeature()
                : persona;
        String visualMemoryContext = visualMemoryService.buildRelevantVisualContext(
                profile == null ? null : profile.getVisualMemoryJson(),
                journalText,
                visualMemoryAnswer
        );

        String prompt = buildImagePrompt(journalText, style, resolvedPersona, visualMemoryContext, contextImages, false);

        try {
            String base64Image = generateBase64Image(prompt);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"base64Image\":\"" + base64Image + "\"}");
        } catch (HttpStatusCodeException e) {
            if (!isRetryableImageSafetyError(e)) {
                return openAiErrorResponse(e);
            }

            try {
                String fallbackPrompt = buildImagePrompt(journalText, style, resolvedPersona, visualMemoryContext, contextImages, true);
                String base64Image = generateBase64Image(fallbackPrompt);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"base64Image\":\"" + base64Image + "\"}");
            } catch (HttpStatusCodeException retryError) {
                return openAiErrorResponse(retryError);
            } catch (Exception retryError) {
                return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        siteTextService.message("ai.error.imageRetryFailed", locale, retryError.getMessage()));
            }
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    siteTextService.message("ai.error.imageRequestFailed", locale, e.getMessage()));
        }
    }

    @PostMapping(
            value = "/journal/handwriting-transcription",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> transcribeHandwriting(@RequestBody Map<String, Object> request) throws JSONException {
        Locale locale = LocaleContextHolder.getLocale();
        if (!openAiKeyProvider.isConfigured()) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, openAiKeyProvider.getConfigurationHelp());
        }

        List<String> images = asStringList(request.get("images")).stream()
                .map(this::asImageDataUrl)
                .filter(image -> image != null && !image.isBlank())
                .distinct()
                .limit(4)
                .toList();

        if (images.isEmpty()) {
            return errorResponse(HttpStatus.BAD_REQUEST, siteTextService.message("ai.error.handwritingMissingImages", locale));
        }

        JSONArray content = new JSONArray();
        content.put(new JSONObject()
                .put("type", "text")
                .put("text",
                        """
                        Transcribe the handwritten journal reflection in the attached image or images.
                        Return only the journal text, preserving paragraph breaks when possible.
                        Do not summarize, translate, explain, correct the writer's meaning, or add any commentary.
                        If a word is unclear, use your best reading instead of adding brackets.
                        """
                ));
        images.forEach(image -> content.put(new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject().put("url", image))));

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are an OCR assistant for private journal photos. Output only the transcribed text."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", content));

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", PROMPT_BUILDER_MODEL_ID);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 900);
        requestBody.put("temperature", 0.1);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiKeyProvider.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    CHAT_URL,
                    new HttpEntity<>(requestBody.toString(), headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return errorResponse(response.getStatusCode(), siteTextService.message("ai.error.emptyResponse", locale));
            }

            JSONObject openaiResponse = new JSONObject(response.getBody());
            String text = openaiResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("text", normalizeTranscriptionText(text)).toString());
        } catch (HttpStatusCodeException e) {
            return openAiErrorResponse(e);
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    siteTextService.message("ai.error.handwritingRequestFailed", locale, e.getMessage()));
        }
    }

    private JSONObject parseStructuredFeedback(String rawContent, String journalEntry, Locale locale) {
        String cleaned = rawContent
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            JSONObject parsed = new JSONObject(cleaned);
            JSONArray questions = parsed.optJSONArray("questions");
            if (questions == null || questions.isEmpty()) {
                questions = new JSONArray(defaultQuestions(journalEntry, locale));
            } else {
                questions = normalizeQuestions(questions, journalEntry, locale);
            }
            parsed.put("questions", questions);
            if (!parsed.has("feedback")) {
                parsed.put("feedback", cleaned);
            }
            parsed.put("sentimentScore", normalizeSentimentScore(parsed.optInt("sentimentScore", defaultSentimentScore(journalEntry)), journalEntry));
            parsed.put("visualMemoryQuestionIndex", normalizeVisualMemoryQuestionIndex(parsed.optInt("visualMemoryQuestionIndex", VISUAL_MEMORY_QUESTION_INDEX), questions.length()));
            return parsed;
        } catch (Exception ignored) {
            return new JSONObject()
                    .put("feedback", cleaned)
                    .put("sentimentScore", defaultSentimentScore(journalEntry))
                    .put("questions", new JSONArray(defaultQuestions(journalEntry, locale)))
                    .put("visualMemoryQuestionIndex", VISUAL_MEMORY_QUESTION_INDEX);
        }
    }

    private List<String> defaultQuestions(String journalEntry, Locale locale) {
        List<String> questions = new ArrayList<>();
        questions.add(siteTextService.message("ai.defaultQuestion.1", locale));
        questions.add(siteTextService.message("ai.defaultQuestion.2", locale));
        questions.add(localizedVisualMemoryQuestion(locale));
        return questions;
    }

    private JSONArray normalizeQuestions(JSONArray questions, String journalEntry, Locale locale) {
        List<String> defaults = defaultQuestions(journalEntry, locale);
        JSONArray normalized = new JSONArray();
        for (int index = 0; index < defaults.size(); index++) {
            String fallback = defaults.get(index);
            String candidate = questions.optString(index, fallback);
            normalized.put(candidate == null || candidate.isBlank() ? fallback : candidate.trim());
        }
        return normalized;
    }

    private int normalizeVisualMemoryQuestionIndex(int index, int questionCount) {
        if (questionCount <= 0) {
            return VISUAL_MEMORY_QUESTION_INDEX;
        }
        return Math.max(0, Math.min(questionCount - 1, index));
    }

    private String localizedVisualMemoryQuestion(Locale locale) {
        return switch (locale.getLanguage()) {
            case "en" -> "Which person, place, or concrete detail from your entry should be described more clearly for a future image?";
            case "nl" -> "Welke persoon, plek of welk concreet detail uit je entry moet duidelijker worden beschreven voor een later beeld?";
            default -> "Welche Person, welcher Ort oder welches konkrete Detail aus deinem Eintrag sollte fÃ¼r ein spÃ¤teres Bild genauer beschrieben werden?";
        };
    }

    private String buildImagePrompt(String journalText, String style, String persona, String visualMemoryContext, List<String> contextImages, boolean aggressiveSanitization) {
        String normalizedStyle = (style == null || style.isBlank()) ? "realistic" : style.toLowerCase(Locale.ROOT);
        String trimmedJournalText = limitText(sanitizeForImagePrompt(journalText, aggressiveSanitization), MAX_JOURNAL_CONTEXT_LENGTH);
        String trimmedPersona = limitText(sanitizeForImagePrompt(persona, aggressiveSanitization), MAX_PERSONA_CONTEXT_LENGTH);
        String trimmedVisualMemoryContext = limitText(sanitizeForImagePrompt(visualMemoryContext, aggressiveSanitization), MAX_VISUAL_MEMORY_CONTEXT_LENGTH);
        List<String> limitedContextImages = limitContextImages(contextImages, trimmedJournalText, trimmedPersona + trimmedVisualMemoryContext);
        String styleInstruction = switch (normalizedStyle) {
            case "watercolor" -> "Create a delicate watercolor illustration with soft edges, layered washes, visible paper texture, atmospheric light, and emotionally rich environmental storytelling.";
            case "cartoon" -> "Create a warm illustrated comic page with 4 clear panels that tell the reflection as one cohesive short narrative.";
            case "bizarre" -> "Create a surreal but cohesive scene where symbolic objects, places, and atmosphere carry the emotion more than characters do.";
            default -> "Create a grounded, softly painterly scene with realistic detail, cinematic light, and strong environmental storytelling.";
        };
        String compositionInstruction = "Render the reflection as one single cohesive frame, not a collage, infographic, profile sheet, poster, character summary, or mood board. ";
        if ("cartoon".equals(normalizedStyle)) {
            compositionInstruction = "Tell the reflection as a short sequence of illustrated panels, while keeping the same emotional story across all panels. ";
        }
        String eventInstruction = "If several moments are mentioned, merge them into one believable place-based scene using representative objects, food, weather, architecture, transit, interiors, nature, and traces of activity instead of separate mini-scenes. ";
        if ("cartoon".equals(normalizedStyle)) {
            eventInstruction = "Let each panel highlight one key moment, while keeping the same day, mood, and visual world throughout. ";
        }
        String peopleInstruction = "Prefer scenes with no visible people. If human presence is necessary, keep people small, distant, turned away, or implied rather than central. Avoid portraits and avoid making the diarist the main subject. ";
        if ("cartoon".equals(normalizedStyle)) {
            peopleInstruction = "Keep characters secondary to the places, objects, and events, and avoid large portrait-style panels. ";
        }
        String textExclusionInstruction =
                "Do not include any writing, letters, numbers, captions, speech bubbles, labels, logos, signs, subtitles, watermarks, handwritten notes, or typographic marks anywhere in the image, including on books, screens, clothing, packaging, and street signs. ";
        String contextPriorityInstruction =
                "The reflection of the day is always the main source of truth. Use persona context or reference images only as fallback hints for unresolved background specifics such as likely city, work context, interior or exterior setting, recurring objects, or clothing, and never let that secondary context overpower the reflection. ";

        String basePrompt = styleInstruction
                + " Capture the emotion through atmosphere, color, weather, place, and objects rather than biography or character design. "
                + compositionInstruction
                + eventInstruction
                + peopleInstruction
                + textExclusionInstruction
                + contextPriorityInstruction
                + "Keep the emotional tone truthful, specific, and cohesive. "
                + (trimmedPersona.isBlank() ? "" : "Fallback background hints only: " + trimmedPersona + ". ")
                + (trimmedVisualMemoryContext.isBlank() ? "" : "Relevant remembered visual cues only if they fit this reflection: " + trimmedVisualMemoryContext + ". ")
                + "Reflection: " + trimmedJournalText;

        if (limitedContextImages.isEmpty()) {
            return clampPrompt(basePrompt);
        }

        try {
            JSONArray content = new JSONArray();
            content.put(new JSONObject()
                    .put("type", "text")
                    .put("text",
                            """
                            Turn the reflection and attached reference images into one concise DALL-E prompt under 650 characters.
                            The reflection is the main source of truth.
                            Prioritize events, setting, atmosphere, weather, food, architecture, transit, interiors, nature, and meaningful objects over people.
                            For every non-comic style, compress the day into one cohesive frame instead of multiple scenes.
                            Avoid portraits and avoid prominent people; prefer empty or lightly populated scenes. If a person must appear, keep them small, distant, or implied.
                            Use persona or reference images only for unresolved background specifics, never as the main subject.
                            Do not include any text, letters, numbers, captions, logos, signs, labels, speech bubbles, or watermarks, even on books, screens, or clothing.
                            Ignore any writing that appears in the references.
                                    Return only the finished prompt text.
                            """
                                    + "\nStyle request: " + normalizedStyle
                                    + "\nReflection: " + trimmedJournalText
                                    + (trimmedPersona.isBlank() ? "" : "\nPersona context: " + trimmedPersona)
                                    + (trimmedVisualMemoryContext.isBlank() ? "" : "\nRelevant visual memory: " + trimmedVisualMemoryContext)
                    ));

            limitedContextImages.stream()
                    .filter(image -> image != null && !image.isBlank())
                    .forEach(image -> content.put(new JSONObject()
                            .put("type", "image_url")
                            .put("image_url", new JSONObject().put("url", image))));

            JSONArray messages = new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", content));

            JSONObject requestBody = new JSONObject()
                    .put("model", PROMPT_BUILDER_MODEL_ID)
                    .put("messages", messages)
                    .put("max_tokens", 220)
                    .put("temperature", 0.4);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openAiKeyProvider.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    CHAT_URL,
                    new HttpEntity<>(requestBody.toString(), headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject promptResponse = new JSONObject(response.getBody());
                String prompt = promptResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();

                if (!prompt.isBlank()) {
                    return clampPrompt(prompt);
                }
            }
        } catch (Exception ignored) {
            // Fall back to text-only prompting if image-to-prompt enrichment fails.
        }

        return clampPrompt(basePrompt);
    }

    private int defaultSentimentScore(String journalEntry) {
        String normalized = journalEntry == null ? "" : journalEntry.toLowerCase(Locale.ROOT);
        int score = 4;

        List<String> positiveSignals = List.of(
                "happy", "calm", "grateful", "hopeful", "relaxed", "good", "joy",
                "glücklich", "glucklich", "ruhig", "dankbar", "hoffnung", "freude", "gut",
                "gelukkig", "rustig", "dankbaar", "hoopvol"
        );
        for (String signal : positiveSignals) {
            if (normalized.contains(signal)) {
                score++;
            }
        }

        List<String> negativeSignals = List.of(
                "sad", "anxious", "worried", "overwhelmed", "angry", "heavy", "tired", "stress",
                "traurig", "ängst", "angst", "sorge", "überfordert", "uberfordert",
                "wüt", "wut", "schwer", "müde", "mude", "gestresst",
                "verdrietig", "bezorgd", "overweldigd", "boos", "zwaar", "moe"
        );
        for (String signal : negativeSignals) {
            if (normalized.contains(signal)) {
                score--;
            }
        }

        return normalizeSentimentScore(score, journalEntry);
    }

    private int normalizeSentimentScore(int rawScore, String journalEntry) {
        if (rawScore < 1 || rawScore > 7) {
            rawScore = 4;
        }
        return Math.max(1, Math.min(7, rawScore));
    }

    private String limitText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private List<String> limitContextImages(List<String> contextImages, String journalText, String persona) {
        if (contextImages == null || contextImages.isEmpty()) {
            return List.of();
        }

        int journalLength = journalText.length();
        int combinedLength = journalText.length() + persona.length();
        int limit;
        if (journalLength > 700 || combinedLength > 1200) {
            limit = 0;
        } else if (journalLength > 360 || combinedLength > 700) {
            limit = 1;
        } else {
            limit = 2;
        }

        if (limit == 0) {
            return List.of();
        }

        return contextImages.stream()
                .map(this::asImageDataUrl)
                .filter(image -> image != null && !image.isBlank())
                .distinct()
                .limit(limit)
                .toList();
    }

    private String sanitizeForImagePrompt(String text, boolean aggressive) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String sanitized = text;
        sanitized = sanitized.replaceAll("(?i)\\bsexy times?\\b", "quiet cuddling");
        sanitized = sanitized.replaceAll("(?i)\\bsex\\b", "cuddling");
        sanitized = sanitized.replaceAll("(?i)\\bsexual\\b", "romantic");
        sanitized = sanitized.replaceAll("(?i)\\bnude\\b", "wrapped in blankets");
        sanitized = sanitized.replaceAll("(?i)\\bnaked\\b", "wrapped in blankets");
        sanitized = sanitized.replaceAll("(?i)\\bsmoked up\\b", "burned incense");
        sanitized = sanitized.replaceAll("(?i)\\bsmoking up\\b", "burning incense");
        sanitized = sanitized.replaceAll("(?i)\\bsmoked a j\\b", "burned incense");
        sanitized = sanitized.replaceAll("(?i)\\bsmoked a (joint|blunt|cigarette|cig)\\b", "burned incense");
        sanitized = sanitized.replaceAll("(?i)\\bhad a j\\b", "had a calm break");
        sanitized = sanitized.replaceAll("(?i)\\bhad a (joint|blunt|cigarette|cig)\\b", "had a calm break");
        sanitized = sanitized.replaceAll("(?i)\\brolled a j\\b", "prepared incense");
        sanitized = sanitized.replaceAll("(?i)\\brolled a (joint|blunt)\\b", "prepared incense");
        sanitized = sanitized.replaceAll("(?i)\\b(weed|cannabis|marijuana)\\b", "incense");
        sanitized = sanitized.replaceAll("(?i)\\bedible(s)?\\b", "snacks");
        sanitized = sanitized.replaceAll("(?i)\\bfuck(?:ed|ing)?\\b", "very");
        sanitized = sanitized.replaceAll("(?i)\\bfck\\b", "very");
        sanitized = sanitized.replaceAll("(?i)\\bshit\\b", "mess");

        if (aggressive) {
            sanitized = sanitized.replaceAll("(?i)\\b(drugs?|drugged|stoned|high|drunk|wasted|tipsy)\\b", "lighthearted");
            sanitized = sanitized.replaceAll("(?i)\\b(erotic|explicit)\\b", "romantic");
            sanitized = sanitized.replaceAll("(?i)\\b(violence|violent)\\b", "tension");
            sanitized = sanitized.replaceAll("(?i)\\b(kill|killed|killing|murder|murdered)\\b", "overcame");
        }

        return sanitized.replaceAll("\\s+", " ").trim();
    }

    private String asImageDataUrl(String image) {
        if (image == null || image.isBlank()) {
            return null;
        }
        if (image.startsWith("data:image")) {
            return image;
        }
        if (image.startsWith("/uploads/") || image.startsWith("uploads/")) {
            return loadUploadImageDataUrl(image);
        }
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image;
        }
        return "data:image/png;base64," + image;
    }

    private String loadUploadImageDataUrl(String imageUrl) {
        String normalized = imageUrl.replace('\\', '/');
        String relativePath = normalized.startsWith("/uploads/")
                ? normalized.substring("/uploads/".length())
                : normalized.substring("uploads/".length());
        Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
        Path file = uploadRoot.resolve(relativePath).toAbsolutePath().normalize();
        if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
            return null;
        }

        try {
            String mime = Files.probeContentType(file);
            if (mime == null || !mime.startsWith("image/")) {
                mime = "image/png";
            }
            byte[] bytes = Files.readAllBytes(file);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String generateBase64Image(String prompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "dall-e-3");
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        body.put("quality", "standard");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiKeyProvider.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                IMAGE_URL,
                new HttpEntity<>(body.toString(), headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI returned no image URL");
        }

        JSONObject jsonResponse = new JSONObject(response.getBody());
        String imageUrl = jsonResponse.getJSONArray("data").getJSONObject(0).getString("url");

        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    private boolean isRetryableImageSafetyError(HttpStatusCodeException e) {
        String response = e.getResponseBodyAsString();
        if (response == null) {
            return false;
        }

        String normalized = response.toLowerCase(Locale.ROOT);
        return normalized.contains("content_policy")
                || normalized.contains("safety")
                || normalized.contains("inappropriate")
                || normalized.contains("policy_violation");
    }

    private String clampPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        String normalized = prompt.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_IMAGE_PROMPT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_IMAGE_PROMPT_LENGTH).trim();
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                results.add(String.valueOf(item));
            }
        }
        return results;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeTranscriptionText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        return raw
                .replace("```text", "")
                .replace("```", "")
                .trim();
    }

    private Profile resolveProfile(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return profileRepository.findByUserUsername(auth.getName()).orElse(null);
    }

    private ResponseEntity<String> openAiErrorResponse(HttpStatusCodeException e) {
        String upstream = e.getResponseBodyAsString();
        String message = upstream == null || upstream.isBlank()
                ? "OpenAI request failed with status " + e.getStatusCode().value()
                : upstream;
        return errorResponse(e.getStatusCode(), message);
    }

    private ResponseEntity<String> errorResponse(HttpStatusCode status, String message) {
        JSONObject out = new JSONObject().put("error", message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(out.toString());
    }

    private String languageNameForPrompt(Locale locale) {
        return switch (locale.getLanguage()) {
            case "en" -> "English";
            case "nl" -> "Dutch";
            default -> "German";
        };
    }

    private boolean containsAny(String content, List<String> signals) {
        return signals.stream().anyMatch(content::contains);
    }
}
