package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.profile.PersonaService;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class JournalService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PersonaService personaService;

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

        // Update persona feature with key elements of this entry
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null) {
            try {
                String updated = personaService.updatePersonaFeature(profile, content);
                profile.setPersonaFeature(updated);
                profileRepository.saveAndFlush(profile);
            } catch (Exception ignore) {
            }
        }
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
        entry.setTitle(title != null ? title : entry.getTitle());
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

    public List<JournalEntrySummary> getEntrySummariesByUser(User user) {
        return journalEntryRepository.findSummariesByUserOrderByTimestampDesc(user);
    }

    public List<JournalEntrySummary> getEntrySummariesByUserAndMonth(
            User user,
            Integer month,
            Integer year
    ) {
        LocalDate now = LocalDate.now();
        int targetMonth = (month == null || month < 1 || month > 12) ? now.getMonthValue() : month;
        int targetYear  = (year  == null || year  < 1) ? now.getYear() : year;

        return journalEntryRepository.findSummariesByUserAndMonthAndYear(user, targetMonth, targetYear);
    }

    public Optional<String> getImageForEntry(Long entryId) {
        return journalEntryRepository.findImageById(entryId)
                .filter(image -> image != null && !image.isBlank());
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

    public List<MonthYearOption> findDistinctMonthsAndYears(User user) {
        List<Object[]> raw = journalEntryRepository.findDistinctMonthsAndYearsByUser(user);
        if (raw.isEmpty()) {
            LocalDate now = LocalDate.now();
            String label = Month.of(now.getMonthValue())
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + now.getYear();
            return List.of(new MonthYearOption(now.getMonthValue(), now.getYear(), label));
        }

        return raw.stream()
                .map(arr -> {
                    int month = ((Number) arr[0]).intValue();
                    int year  = ((Number) arr[1]).intValue();
                    String label = Month.of(month)
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
                    return new MonthYearOption(month, year, label);
                })
                .toList();
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
