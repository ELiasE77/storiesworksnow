package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/journal")
@CrossOrigin(origins = "http://localhost:8080")
public class JournalEntryController {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public JournalEntry createJournalEntry(@RequestBody JournalEntry entry) {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        Optional<User> user = userRepository.findByUsername(username);
        entry.setUser(user);
        return journalEntryRepository.save(entry);
    }

    @GetMapping
    public List<JournalEntry> getAllEntries() {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        Optional<User> user = userRepository.findByUsername(username);
        return journalEntryRepository.findByUserOrderByEntryDateDesc(user);
    }
}


