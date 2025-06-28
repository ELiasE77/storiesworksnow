package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JournalService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private CommunityRepository communityRepository;

    public void saveJournalEntry(
            User user,
            String title,
            String content,
            String imageUrl,
            Long communityId,
            JournalEntry.Visibility visibility
    ) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setTimestamp(LocalDateTime.now());
        entry.setUser(user);
        entry.setVisibility(visibility);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            entry.setImageUrl(imageUrl);
        }
        if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
            Community c = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Community not found"));
            entry.setCommunity(c);
        }
        journalEntryRepository.save(entry);
    }

    public void updateJournalEntry(
            Long id,
            String title,
            String content,
            String imageUrl,
            JournalEntry.Visibility visibility,
            Long communityId
    ) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found"));
        entry.setTitle(title);
        entry.setContent(content);
        entry.setVisibility(visibility);
        entry.setImageUrl(imageUrl);

        if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
            Community c = communityRepository.findById(communityId)
                    .orElseThrow(() -> new RuntimeException("Community not found"));
            entry.setCommunity(c);
        }
        journalEntryRepository.save(entry);
    }

    public List<JournalEntry> getEntriesByUser(User user) {
        return journalEntryRepository.findByUser(user);
    }

    public JournalEntry findJournalEntryById(Long id) throws ResourceNotFoundException {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found: " + id));
    }

    public List<JournalEntry> findAllEntriesSortedByTimestamp() {
        return journalEntryRepository.findAllByOrderByTimestampDesc();
    }

    public List<Object[]> findMonthAndYear(User user) {
        return journalEntryRepository.findDistinctMonthsAndYearsWithImages(user);
    }

    public List<JournalEntry> findEntriesForMonthAndYear(User user, Integer month, Integer year) {
        return journalEntryRepository.findByUserAndMonthAndYearWithImages(user, month, year);
    }

    public List<JournalEntry> findCommunityEntries(Community community) {
        return journalEntryRepository.findByCommunityOrderByTimestampDesc(community);
    }

    public void deleteJournalEntry(Long id) {
        journalEntryRepository.deleteById(id);
    }

    public List<JournalEntry> findPublicEntriesSortedByTimestamp() {
        return journalEntryRepository.findByVisibilityOrderByTimestampDesc(JournalEntry.Visibility.PUBLIC);
    }
}
