package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReflectionService {
    private final ReflectionMessageRepository repo;
    private final JournalEntryRepository entryRepo;

    public ReflectionService(ReflectionMessageRepository repo,
                             JournalEntryRepository entryRepo) {
        this.repo = repo;
        this.entryRepo = entryRepo;
    }

    public JournalEntry findEntry(Long id) {
        return entryRepo.findById(id).orElseThrow();
    }

    public List<ReflectionMessage> getMessages(JournalEntry entry) {
        return repo.findByEntryOrderByTimestampAsc(entry);
    }

    public ReflectionMessage saveMessage(JournalEntry entry,
                                         ReflectionMessage.Role role,
                                         String content) {
        ReflectionMessage m = new ReflectionMessage();
        m.setEntry(entry);
        m.setRole(role);
        m.setContent(content);
        m.setTimestamp(LocalDateTime.now());
        return repo.save(m);
    }
}