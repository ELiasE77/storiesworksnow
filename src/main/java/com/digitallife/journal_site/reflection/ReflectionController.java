package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    private static final String OPENAI_KEY = "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";
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_ID = "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";

    private final ReflectionService service;

    public ReflectionController(ReflectionService service) {
        this.service = service;
    }

    @PostMapping("/{entryId}/message")
    public ResponseEntity<String> converse(@PathVariable Long entryId,
                                           @RequestBody Map<String, String> body) {
        String userMsg = body.getOrDefault("message", "");
        JournalEntry entry = service.findEntry(entryId);
        service.saveMessage(entry, ReflectionMessage.Role.USER, userMsg);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system")
                .put("content", "You are a journaling assistant helping the user reflect on their entry."));
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
        headers.set("Authorization", "Bearer " + OPENAI_KEY);
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

        JSONObject out = new JSONObject().put("reply", reply);
        return ResponseEntity.ok(out.toString());
    }
}
