package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    @Value("${openai.api.key:}")
    private String openaiApiKey;
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_ID = "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";

    private final ReflectionService service;

    public ReflectionController(ReflectionService service) {
        this.service = service;
    }

    @PostMapping("/{entryId}/message")
    public ResponseEntity<String> converse(@PathVariable Long entryId,
                                           @RequestBody Map<String, String> body) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return ResponseEntity.status(500).body("{\"error\":\"OpenAI API key is not set\"}");
        }
        String userMsg = body.getOrDefault("message", "");
        if (userMsg == null || userMsg.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Message cannot be empty\"}");
        }
        JournalEntry entry = service.findEntry(entryId);

        String messageIdRaw = body.get("messageId");
        if (messageIdRaw != null && !messageIdRaw.isBlank()) {
            try {
                Long messageId = Long.valueOf(messageIdRaw);
                service.updateUserMessage(entry, messageId, userMsg);
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().body("{\"error\":\"Invalid message id\"}");
            }
        } else {
            service.saveMessage(entry, ReflectionMessage.Role.USER, userMsg);
        }

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system")
                .put("content",
                        "You are a journaling assistant helping the user reflect on their entry." +
                                " Offer concise, empathetic suggestions that encourage growth." +
                                " Blend persona knowledge and past lessons when responding."));

        String personaContext = service.buildPersonaContext(entry);
        if (!personaContext.isBlank()) {
            messages.put(new JSONObject().put("role", "system")
                    .put("content", personaContext));
        }

        Long userId = entry.getUser() != null ? entry.getUser().getId() : null;
        String reinforcement = service.buildReinforcementContext(userId, entry.getId(), 12);
        if (!reinforcement.isBlank()) {
            messages.put(new JSONObject().put("role", "system")
                    .put("content", reinforcement));
        }

        messages.put(new JSONObject().put("role", "user")
                .put("content", "Journal entry:" + entry.getContent()));

        List<ReflectionMessage> prev = service.getMessages(entry);
        for (ReflectionMessage m : prev) {
            messages.put(new JSONObject()
                    .put("role", m.getRole() == ReflectionMessage.Role.USER ? "user" : "assistant")
                    .put("content", m.getContent()));
        }

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("model", MODEL_ID);
        bodyJson.put("messages", messages);
        bodyJson.put("max_tokens", 150);
        bodyJson.put("temperature", 0.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);
        HttpEntity<String> req = new HttpEntity<>(bodyJson.toString(), headers);
        RestTemplate rest = new RestTemplate();
        ResponseEntity<String> resp = rest.postForEntity(CHAT_URL, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        }

        JSONObject json = new JSONObject(resp.getBody());
        String reply = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        service.saveMessage(entry, ReflectionMessage.Role.ASSISTANT, reply);

        JSONObject out = new JSONObject()
                .put("reply", reply)
                .put("messages", toHistoryArray(service.getMessages(entry)));
        return ResponseEntity.ok(out.toString());
    }

    @GetMapping("/{entryId}/history")
    public ResponseEntity<String> history(@PathVariable Long entryId) {
        JournalEntry entry = service.findEntry(entryId);
        JSONObject out = new JSONObject()
                .put("messages", toHistoryArray(service.getMessages(entry)));
        return ResponseEntity.ok(out.toString());
    }

    private JSONArray toHistoryArray(List<ReflectionMessage> history) {
        JSONArray arr = new JSONArray();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (ReflectionMessage msg : history) {
            arr.put(new JSONObject()
                    .put("id", msg.getId())
                    .put("role", msg.getRole().name())
                    .put("content", msg.getContent())
                    .put("timestamp", msg.getTimestamp() != null ? msg.getTimestamp().format(formatter) : ""));
        }
        return arr;
    }
}
