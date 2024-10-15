package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JournalService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    public JournalEntry saveJournalEntry(User user, String content) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setUser(user);
        journalEntry.setContent(content);
        journalEntry.setTimestamp(LocalDateTime.now());
        return journalEntryRepository.save(journalEntry);
    }

    public List<JournalEntry> getEntriesByUser(User user) {
        return journalEntryRepository.findByUser(user);
    }
}


