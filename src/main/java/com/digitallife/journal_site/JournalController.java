package com.digitallife.journal_site;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/journal")
public class JournalController {
    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping
    public ResponseEntity<?> handleJournalEntry(@RequestBody Map<String, String> payload) {
        String entry = payload.get("entry");
        String aiResponse = journalService.processEntry(entry);
        return ResponseEntity.ok(Collections.singletonMap("message", aiResponse));
    }
}
