package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.common.SiteTextService;
import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.profile.PersonaService;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.profile.VisualMemoryService;
import com.digitallife.journal_site.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
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

    @Autowired
    private VisualMemoryService visualMemoryService;

    @Autowired
    private SiteTextService siteTextService;

    @Autowired
    private JournalImageStorageService imageStorageService;

    @Autowired
    private JournalAudioStorageService audioStorageService;

    public JournalEntry saveJournalEntry(
            User user,
            String title,
            String content,
            String imageUrl,
            List<String> imageUrls,
            String voiceMemoAudioDataUrl,
            JournalEntryData entryData,
            Long communityId,
            JournalEntry.Visibility visibility
    ) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle(title);
        entry.setContent(content == null ? "" : content);
        entry.setTimestamp(LocalDateTime.now());
        entry.setUser(user);
        entry.setVisibility(visibility);
        entry.setEntryData(siteTextService.normalizeEntryData(entryData));

            if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
                Community c = communityRepository.findById(communityId)
                        .orElseThrow(() -> new RuntimeException("Community not found"));
                entry.setCommunity(c);
            }
            JournalEntry savedEntry = journalEntryRepository.saveAndFlush(entry);
            applyStoredImages(savedEntry, imageUrl, imageUrls);
            applyStoredAudio(savedEntry, voiceMemoAudioDataUrl);
            savedEntry = journalEntryRepository.save(savedEntry);

            // Update persona feature with key elements of this entry
            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                try {
                    String updated = personaService.updatePersonaFeature(profile, savedEntry.getContent());
                    profile.setPersonaFeature(updated);
                } catch (Exception ignore) {
                }

                try {
                    profile.setVisualMemoryJson(
                            visualMemoryService.upsertVisualMemory(profile.getVisualMemoryJson(), savedEntry.getId(), entry.getEntryData())
                    );
                } catch (Exception ignore) {
                }

                profileRepository.saveAndFlush(profile);
            }
            return savedEntry;
        }

        public void updateJournalEntry(
                Long id,
                String title,
                String content,
                String imageUrl,
                List<String> imageUrls,
                JournalEntryData entryData,
                JournalEntry.Visibility visibility,
                Long communityId
    ) {
            JournalEntry entry = journalEntryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entry not found"));
            entry.setTitle(title != null ? title : entry.getTitle());
            entry.setContent(content == null ? "" : content);
            entry.setVisibility(visibility);
            entry.setEntryData(siteTextService.normalizeEntryData(entryData));

            applyStoredImages(entry, imageUrl, imageUrls);

            if (visibility == JournalEntry.Visibility.COMMUNITY && communityId != null) {
                Community c = communityRepository.findById(communityId)
                        .orElseThrow(() -> new RuntimeException("Community not found"));
                entry.setCommunity(c);
            } else {
                entry.setCommunity(null);
            }
            journalEntryRepository.save(entry);

            Profile profile = profileRepository.findByUserId(entry.getUser().getId()).orElse(null);
            if (profile != null) {
                try {
                    profile.setVisualMemoryJson(
                            visualMemoryService.upsertVisualMemory(profile.getVisualMemoryJson(), entry.getId(), entry.getEntryData())
                    );
                    profileRepository.saveAndFlush(profile);
                } catch (Exception ignore) {
                }
            }
        }

    public List<JournalEntry> findPublicEntriesSortedByTimestamp() {
        return journalEntryRepository
                .findByVisibilityOrderByTimestampDesc(JournalEntry.Visibility.PUBLIC);
    }

        private void applyStoredImages(JournalEntry entry, String imageUrl, List<String> imageUrls) {
            JournalImageStorageService.StoredJournalImages storedImages = imageStorageService.storeImages(
                    entry.getId(),
                    imageUrl,
                    imageUrls,
                    entry.getThumbnailUrl()
            );

            List<String> images = storedImages.imageUrls();
            entry.setImageUrl(images.isEmpty() ? null : images.get(0));
            entry.setImageUrlsJson(images.isEmpty() ? null : toJson(images));
            entry.setThumbnailUrl(storedImages.thumbnailUrl());
        }

        private void applyStoredAudio(JournalEntry entry, String audioDataUrl) {
            JournalEntryData entryData = entry.getEntryData();
            String storedAudioUrl = audioStorageService.storeAudio(
                    entry.getId(),
                    audioDataUrl,
                    entryData.getVoiceMemoAudioUrl()
            );
            entryData.setVoiceMemoAudioUrl(storedAudioUrl);
            entry.setEntryData(siteTextService.normalizeEntryData(entryData));
        }

        private List<String> sanitizeImages(List<String> imageUrls) {
            if (imageUrls == null) {
                return new ArrayList<>();
            }

            return imageUrls.stream()
                    .map(this::normalizeImagePayload)
                    .filter(image -> image != null && !image.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        private String normalizeImagePayload(String image) {
            if (image == null || image.isBlank()) {
                return null;
            }

            String normalized = image.trim();
            if (normalized.startsWith("data:image")) {
                return normalized;
            }
            return "data:image/png;base64," + normalized;
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

        public List<JournalEntry> getEntriesForUser(User user) {
            return journalEntryRepository.findByUserOrderByTimestampDesc(user);
        }

        public JournalEntry findJournalEntryById(Long id) throws ResourceNotFoundException {
            return journalEntryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found: " + id));
        }

        public List<JournalEntry> findAllEntriesSortedByTimestamp() {
            return journalEntryRepository.findAllByOrderByTimestampDesc();
        }

        public List<JournalEntryListItem> findEntryItemsForMonth(User user, YearMonth yearMonth) {
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
            return journalEntryRepository.findListItemsByUserAndTimestampRange(user, start, end)
                    .stream()
                    .map(JournalEntryListItem::from)
                    .toList();
        }

        public List<JournalEntryListItem> findRecentEntryItems(User user, Pageable pageable) {
            return journalEntryRepository.findRecentListItemsByUser(user, pageable)
                    .stream()
                    .map(JournalEntryListItem::from)
                    .toList();
        }

        public List<JournalEntryListItem> findAllEntryItems(User user) {
            return findRecentEntryItems(user, Pageable.unpaged());
        }

        public JournalEntry findMostRecentEntry(User user) {
            return journalEntryRepository.findFirstByUserOrderByTimestampDesc(user);
        }

        public JournalEntryListItem findMostRecentEntryItem(User user) {
            return findRecentEntryItems(user, Pageable.ofSize(1))
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        public List<JournalEntryListItem> findPublicEntryItems(Pageable pageable) {
            return journalEntryRepository
                    .findPublicListItems(JournalEntry.Visibility.PUBLIC, pageable)
                    .stream()
                    .map(JournalEntryListItem::from)
                    .toList();
        }

        public List<JournalEntryListItem> findCommunityEntryItems(Community community, Pageable pageable) {
            return journalEntryRepository.findCommunityListItems(community, pageable)
                    .stream()
                    .map(JournalEntryListItem::from)
                    .toList();
        }

        public LocalDateTime findLatestTimestampByUser(User user) {
            return journalEntryRepository.findLatestTimestampByUser(user);
        }

        public List<LocalDateTime> findEntryTimestamps(User user) {
            return journalEntryRepository.findTimestampsByUser(user);
        }

        public List<JournalInsightItem> findInsightItems(User user) {
            return journalEntryRepository.findInsightItemsByUser(user)
                    .stream()
                    .map(JournalInsightItem::from)
                    .toList();
        }

        public long countEntriesByUser(User user) {
            return journalEntryRepository.countByUser(user);
        }

        public List<Object[]> findMonthAndYear(User user) {
            return journalEntryRepository.findDistinctMonthsAndYearsWithImages(user);
        }

        public List<Object[]> findEntryMonthAndYear(User user) {
            return journalEntryRepository.findDistinctMonthsAndYears(user);
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
