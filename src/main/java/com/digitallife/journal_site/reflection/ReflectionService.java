package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalEntryRepository;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReflectionService {
    private final ReflectionMessageRepository repo;
    private final JournalEntryRepository entryRepo;
    private final ProfileRepository profileRepo;

    public ReflectionService(ReflectionMessageRepository repo,
        JournalEntryRepository entryRepo,
        ProfileRepository profileRepo) {
            this.repo = repo;
            this.entryRepo = entryRepo;
            this.profileRepo = profileRepo;
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

        @Transactional
        public ReflectionMessage updateUserMessage(JournalEntry entry,
                Long messageId,
                String content) {
            ReflectionMessage message = repo.findByIdAndEntryId(messageId, entry.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Reflection message not found"));
            if (message.getRole() != ReflectionMessage.Role.USER) {
                throw new IllegalArgumentException("Only user reflections can be edited");
            }
            message.setContent(content);
            message.setTimestamp(LocalDateTime.now());
            ReflectionMessage updated = repo.save(message);
            repo.deleteMessagesAfter(entry, updated.getId());
            return updated;
        }

        public Optional<Profile> findProfileForEntry(JournalEntry entry) {
            if (entry.getUser() == null) {
                return Optional.empty();
            }
            return profileRepo.findByUserId(entry.getUser().getId());
        }

        public String buildPersonaContext(JournalEntry entry) {
            return findProfileForEntry(entry)
                    .map(profile -> {
                        StringBuilder sb = new StringBuilder();
                        if (!isBlank(profile.getPersonaFeature())) {
                            sb.append("Persona biography:\n").append(profile.getPersonaFeature().trim()).append('\n');
                        }
                        if (!isBlank(profile.getPersona())) {
                            sb.append("Questionnaire outlook:\n").append(profile.getPersona().trim()).append('\n');
                        }
                        if (!isBlank(profile.getAppearanceJson())) {
                            sb.append("Persona file data:\n").append(profile.getAppearanceJson().trim()).append('\n');
                        }
                        return sb.toString().trim();
                    })
                    .orElse("");
        }

        public String buildReinforcementContext(Long userId, Long currentEntryId, int limit) {
            if (userId == null) {
                return "";
            }
            List<ReflectionMessage> recent = repo.findByEntryUserIdOrderByTimestampDesc(
                    userId,
                    PageRequest.of(0, limit)
            );
            if (recent.isEmpty()) {
                return "";
            }

            List<ReflectionMessage> filtered = recent.stream()
                    .filter(msg -> currentEntryId == null || !msg.getEntry().getId().equals(currentEntryId))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                return "";
            }

            Collections.reverse(filtered);
            StringBuilder sb = new StringBuilder("Recent reflection takeaways to reinforce:\n");
            for (ReflectionMessage msg : filtered) {
                sb.append(msg.getRole() == ReflectionMessage.Role.ASSISTANT ? "Coach" : "Journaler")
                        .append(':')
                        .append(' ')
                        .append(truncate(msg.getContent().replaceAll("\s+", " "), 240))
                        .append('\n');
            }
            return sb.toString().trim();
        }

        private boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }

        private String truncate(String value, int maxLength) {
            if (value.length() <= maxLength) {
                return value;
            }
            return value.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
        }
    }