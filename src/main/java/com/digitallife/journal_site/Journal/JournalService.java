package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JournalService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private CommunityRepository communityRepository;

    public void saveJournalEntry(User user, String title, String content, String imageUrl, Long communityId, JournalEntry.Visibility visibility) {
        JournalEntry journalEntry = new JournalEntry();

        if (title != null && !title.isEmpty()) {
            journalEntry.setTitle(title);
        }

        System.out.println(communityId);

        if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
            Community community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Community not found"));
            journalEntry.setCommunity(community);
        }

        journalEntry.setVisibility(visibility);
        journalEntry.setUser(user);
        journalEntry.setContent(content);
        journalEntry.setTimestamp(LocalDateTime.now());

        if (imageUrl != null) {
            journalEntry.setImage(imageUrl);
        }
        journalEntryRepository.save(journalEntry);
    }

    public void updateJournalEntry(Long id, String title, String content, String imageUrl, JournalEntry.Visibility visibility, Long communityId) {
        JournalEntry entry = journalEntryRepository.findById(id).orElseThrow(() -> new RuntimeException("Entry not found"));
        entry.setTitle(title);
        entry.setContent(content);
        entry.setImage(imageUrl);
        entry.setVisibility(visibility);
        if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
            Community community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Community not found"));
            entry.setCommunity(community);
        }
        journalEntryRepository.save(entry);
    }

    public List<JournalEntry> getEntriesByUser(User user) {
        return journalEntryRepository.findByUser(user);
    }

    public JournalEntry findJournalEntryById(Long id) throws ResourceNotFoundException {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found with id: " + id));
    }

    public List<JournalEntry> findAllEntriesSortedByTimestamp() {
        return journalEntryRepository.findAllByOrderByTimestampDesc();
    }
}


