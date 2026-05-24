package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.ChatGptIntegration.OpenAiKeyProvider;
import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.common.SiteTextService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_ID = "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";

    private final ReflectionService service;
    private final OpenAiKeyProvider openAiKeyProvider;
    private final SiteTextService siteTextService;

    public ReflectionController(ReflectionService service,
                                OpenAiKeyProvider openAiKeyProvider,
                                SiteTextService siteTextService) {
        this.service = service;
        this.openAiKeyProvider = openAiKeyProvider;
        this.siteTextService = siteTextService;
    }

    @PostMapping("/{entryId}/message")
    public ResponseEntity<String> converse(@PathVariable Long entryId,
                                           @RequestBody Map<String, String> body) {
        Locale locale = LocaleContextHolder.getLocale();
        if (!openAiKeyProvider.isConfigured()) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, openAiKeyProvider.getConfigurationHelp());
        }

        String userMsg = body.getOrDefault("message", "");
        JournalEntry entry = service.findEntry(entryId);
        service.saveMessage(entry, ReflectionMessage.Role.USER, userMsg);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system")
                .put("content",
                        "You are a journaling assistant helping the user reflect on their entry. "
                                + "Always reply in " + languageNameForPrompt(locale) + "."));
        messages.put(new JSONObject().put("role", "user")
                .put("content", "Journal entry:" + entry.getContent()));

        List<ReflectionMessage> previousMessages = service.getMessages(entry);
        for (ReflectionMessage message : previousMessages) {
            messages.put(new JSONObject()
                    .put("role", message.getRole() == ReflectionMessage.Role.USER ? "user" : "assistant")
                    .put("content", message.getContent()));
        }

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("model", MODEL_ID);
        bodyJson.put("messages", messages);
        bodyJson.put("max_tokens", 150);
        bodyJson.put("temperature", 0.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKeyProvider.getApiKey());

        ResponseEntity<String> response;
        try {
            response = new RestTemplate().postForEntity(
                    CHAT_URL,
                    new HttpEntity<>(bodyJson.toString(), headers),
                    String.class
            );
        } catch (HttpStatusCodeException e) {
            return errorResponse(e.getStatusCode(), readableMessage(e.getResponseBodyAsString()));
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    siteTextService.message("reflection.error.requestFailed", locale, e.getMessage()));
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return errorResponse(response.getStatusCode(), siteTextService.message("reflection.error.emptyResponse", locale));
        }

        try {
            JSONObject json = new JSONObject(response.getBody());
            String reply = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

            service.saveMessage(entry, ReflectionMessage.Role.ASSISTANT, reply);
            return ResponseEntity.ok(new JSONObject().put("reply", reply).toString());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, siteTextService.message("reflection.error.parse", locale));
        }
    }

    private ResponseEntity<String> errorResponse(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("error", message).toString());
    }

    private String readableMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "OpenAI returned an empty error response";
        }
        return responseBody;
    }

    private String languageNameForPrompt(Locale locale) {
        return switch (locale.getLanguage()) {
            case "en" -> "English";
            case "nl" -> "Dutch";
            default -> "German";
        };
    }
}
