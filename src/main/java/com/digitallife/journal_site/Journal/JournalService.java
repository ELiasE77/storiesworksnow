package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.profile.PersonaService;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
            List<String> imageUrls,
            Long communityId,
            JournalEntry.Visibility visibility
    ) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setTimestamp(LocalDateTime.now());
        entry.setUser(user);
        entry.setVisibility(visibility);

            List<String> galleryImages = sanitizeImages(imageUrls);
            if (imageUrl != null && !imageUrl.isBlank() && !galleryImages.contains(imageUrl)) {
                galleryImages.add(imageUrl);
            }

            if (!galleryImages.isEmpty()) {
                entry.setImageUrl(galleryImages.get(0));
                entry.setImageUrlsJson(toJson(galleryImages));
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
                List<String> imageUrls,
                JournalEntry.Visibility visibility,
                Long communityId
    ) {
            JournalEntry entry = journalEntryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entry not found"));
            entry.setTitle(title != null ? title : entry.getTitle());
            entry.setContent(content);
            entry.setVisibility(visibility);

            List<String> galleryImages = sanitizeImages(imageUrls);
            if (imageUrl != null && !imageUrl.isBlank() && !galleryImages.contains(imageUrl)) {
                galleryImages.add(imageUrl);
            }

            entry.setImageUrl(galleryImages.isEmpty() ? null : galleryImages.get(0));
            entry.setImageUrlsJson(galleryImages.isEmpty() ? null : toJson(galleryImages));

            if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
                Community c = communityRepository.findById(communityId)
                        .orElseThrow(() -> new RuntimeException("Community not found"));
                entry.setCommunity(c);
            }
            journalEntryRepository.save(entry);
        }

    public List<JournalEntry> findPublicEntriesSortedByTimestamp() {
        return journalEntryRepository
                .findByVisibilityOrderByTimestampDesc(JournalEntry.Visibility.PUBLIC);
    }

        private List<String> sanitizeImages(List<String> imageUrls) {
            if (imageUrls == null) {
                return new ArrayList<>();
            }

            return imageUrls.stream()
                    .filter(image -> image != null && !image.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        private String toJson(List<String> imageUrls) {
            try {
                return new ObjectMapper().writeValueAsString(imageUrls);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize entry images", e);
            }
        }

        public List<JournalEntry> getEntriesByUser(User user) {
            return journalEntryRepository.findByUserOrderByTimestampDesc(user);
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
            journalEntryRepository.deleteById(id);     }
}