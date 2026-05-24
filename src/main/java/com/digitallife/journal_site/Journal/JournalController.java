package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.ChatGptIntegration.ChatGPTController;
import com.digitallife.journal_site.common.SiteTextService;
import com.digitallife.journal_site.communities.CommunityService;
import com.digitallife.journal_site.profile.CompanionCatalog;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class JournalController {

    private final JournalService journalService;
    private final JournalInsightsService journalInsightsService;
    private final UserDetailService userService;
    private final CommunityService communityService;
    @SuppressWarnings("unused")
    private final ChatGPTController chatGptController;
    private final ProfileRepository profileRepository;
    private final SiteTextService siteTextService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JournalController(JournalService journalService,
                             JournalInsightsService journalInsightsService,
                             UserDetailService userService,
                             CommunityService communityService,
                             ChatGPTController chatGptController,
                             ProfileRepository profileRepository,
                             SiteTextService siteTextService) {
        this.journalService = journalService;
        this.journalInsightsService = journalInsightsService;
        this.userService = userService;
        this.communityService = communityService;
        this.chatGptController = chatGptController;
        this.profileRepository = profileRepository;
        this.siteTextService = siteTextService;
    }

    @GetMapping("/journal")
    public String showCreateForm(Model model, Authentication auth) {
        model.addAttribute("communities", communityService.findCommunitySummariesByUsername(auth.getName()));
        Profile profile = profileRepository.findByUserUsername(auth.getName()).orElse(null);
        model.addAttribute("personaFeature", profile == null ? "" : profile.getPersonaFeature());
        model.addAttribute("activeCompanion", CompanionCatalog.find(profile == null ? null : profile.getCompanionKey()));
        return "journaling/journal";
    }

    @PostMapping(
            value = "/api/journal/save",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<String> saveViaJson(
            @RequestBody JournalEntryDTO dto,
            Authentication auth,
            Locale locale
    ) throws Exception {
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity
                    .badRequest()
                    .body("{\"error\":\"" + siteTextService.message("journal.error.userNotFound", locale, auth.getName()) + "\"}");
        }

        if (dto.getVisibility() == null) {
            dto.setVisibility(JournalEntry.Visibility.PRIVATE);
        }

        JournalEntry savedEntry = journalService.saveJournalEntry(
                user,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageUrl(),
                dto.getImageUrls(),
                dto.getVoiceMemoAudioDataUrl(),
                dto.getEntryData(),
                dto.getCommunityId(),
                dto.getVisibility()
        );

        JSONObject resp = new JSONObject()
                .put("status", "ok")
                .put("entryId", savedEntry.getId())
                .put("redirectUrl", "/");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp.toString());
    }

    @GetMapping("/journal/home")
    public String showUserJournals(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "period", required = false) String period,
            Model model,
            Principal principal,
            Locale locale
    ) {
        User user = userService.findByUsername(principal.getName());
        List<JournalEntryListItem> entries = journalService.findAllEntryItems(user);

        model.addAttribute("entries", entries);
        model.addAttribute("entryCount", entries.size());
        return "user/UserJournalEntries";
    }

    @GetMapping(value = "/api/journal/{id}/images", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> loadJournalEntryImages(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("images", List.of()));
        }

        User user = userService.findByUsername(principal.getName());
        JournalEntry entry;
        try {
            entry = journalService.findJournalEntryById(id);
        } catch (Exception ignored) {
            return ResponseEntity.notFound().build();
        }

        if (entry.getUser() == null || !entry.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("images", List.of()));
        }

        return ResponseEntity.ok(Map.of("images", entry.getDisplayImageGallery()));
    }

    @GetMapping("/journal/entry")
    public String showJournalEntry(@RequestParam("id") Long id, Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        JournalEntry entry = journalService.findJournalEntryById(id);
        if (!entry.getUser().getId().equals(user.getId())) {
            return "redirect:/journal/home";
        }

        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        model.addAttribute("entry", entry);
        model.addAttribute("activeCompanion", CompanionCatalog.find(profile == null ? null : profile.getCompanionKey()));
        return "journaling/journal_entry";
    }

    @GetMapping("/journal/sharePage")
    public String getAllJournalEntries(Model model) {
        List<JournalEntryListItem> entries = journalService.findPublicEntryItems(PageRequest.of(0, 20));
        model.addAttribute("entries", entries);
        return "social/social_home";
    }

    @GetMapping("/journal/timeline")
    public String showImageTimeline(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model,
            Principal principal
    ) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("availableMonthsAndYears",
                journalService.findMonthAndYear(user));

        LocalDateTime now = LocalDateTime.now();
        int selMonth = (month == null) ? now.getMonthValue() : month;
        int selYear = (year == null) ? now.getYear() : year;

        List<JournalEntry> entries =
                journalService.findEntriesForMonthAndYear(user, selMonth, selYear);

        model.addAttribute("selectedMonth", selMonth);
        model.addAttribute("selectedYear", selYear);
        model.addAttribute("entries", entries);
        return "journaling/timeline";
    }

    @GetMapping("/journal/edit")
    public String editJournalEntry(
            @RequestParam("id") Long id,
            Model model,
            Authentication auth
    ) {
        JournalEntry entry = journalService.findJournalEntryById(id);
        model.addAttribute("entry", entry);
        model.addAttribute("entryDataJson", writeEntryData(siteTextService.normalizeEntryData(entry.getEntryData())));
        model.addAttribute("communities", communityService.findCommunitySummariesByUsername(auth.getName()));

        Profile profile = profileRepository.findByUserUsername(auth.getName()).orElse(null);
        model.addAttribute("personaFeature", profile == null ? "" : profile.getPersonaFeature());
        model.addAttribute("activeCompanion", CompanionCatalog.find(profile == null ? null : profile.getCompanionKey()));
        return "journaling/journal_editing";
    }

    @PostMapping("/journal/update")
    public String updateJournalEntry(
            @RequestParam("id") Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("content") String content,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "imageUrlsJson", required = false) String imageUrlsJson,
            @RequestParam(value = "entryDataJson", required = false) String entryDataJson,
            @RequestParam("visibility") JournalEntry.Visibility visibility,
            @RequestParam(value = "communityId", required = false) String communityIdStr
    ) {
        List<String> imageUrls = new ArrayList<>();
        if (imageUrlsJson != null && !imageUrlsJson.isBlank()) {
            try {
                imageUrls = objectMapper.readValue(
                        imageUrlsJson,
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception ignore) {
                imageUrls = new ArrayList<>();
            }
        }

        Long communityId = null;
        if (communityIdStr != null && !communityIdStr.isBlank()) {
            try {
                communityId = Long.valueOf(communityIdStr);
            } catch (NumberFormatException ignore) {
                communityId = null;
            }
        }

        journalService.updateJournalEntry(
                id,
                title,
                content,
                imageUrl,
                imageUrls,
                parseEntryData(entryDataJson),
                visibility,
                communityId
        );
        return "redirect:/journal/entry?id=" + id;
    }

    @PostMapping("/journal/delete")
    public String deleteJournalEntry(@RequestParam("id") Long id) {
        journalService.deleteJournalEntry(id);
        return "redirect:/journal/home";
    }

    private JournalEntryData parseEntryData(String entryDataJson) {
        if (entryDataJson == null || entryDataJson.isBlank()) {
            return new JournalEntryData();
        }

        try {
            return objectMapper.readValue(entryDataJson, JournalEntryData.class);
        } catch (Exception ignored) {
            return new JournalEntryData();
        }
    }

    private List<ArchiveMonthGroup> buildArchiveGroups(List<JournalEntry> entries, Locale locale) {
        List<ArchiveMonthGroup> grouped = new ArrayList<>();
        for (Map.Entry<String, List<JournalEntry>> group : journalInsightsService.groupEntriesByMonth(entries).entrySet()) {
            grouped.add(new ArchiveMonthGroup(
                    group.getKey(),
                    formatArchiveLabel(group.getKey(), locale),
                    group.getValue()
            ));
        }
        return grouped;
    }

    private YearMonth resolveSelectedMonth(User user, Integer month, Integer year, String period) {
        if (period != null && !period.isBlank()) {
            try {
                return YearMonth.parse(period);
            } catch (Exception ignored) {
            }
        }

        if (month != null && year != null && month >= 1 && month <= 12) {
            return YearMonth.of(year, month);
        }

        LocalDateTime latestTimestamp = journalService.findLatestTimestampByUser(user);
        return latestTimestamp == null ? YearMonth.now() : YearMonth.from(latestTimestamp);
    }

    private List<JournalMonthOption> buildMonthOptions(User user, Locale locale) {
        return journalService.findEntryMonthAndYear(user).stream()
                .map(this::toYearMonth)
                .filter(value -> value != null)
                .map(yearMonth -> monthOption(yearMonth, locale))
                .toList();
    }

    private YearMonth toYearMonth(Object[] row) {
        if (row == null || row.length < 2 || !(row[0] instanceof Number month) || !(row[1] instanceof Number year)) {
            return null;
        }
        return YearMonth.of(year.intValue(), month.intValue());
    }

    private JournalMonthOption monthOption(YearMonth yearMonth, Locale locale) {
        return new JournalMonthOption(yearMonth, formatArchiveLabel(yearMonth.toString(), locale));
    }

    private List<JournalDayGroup> buildDayGroups(List<JournalEntryListItem> entries, Locale locale) {
        Map<LocalDate, List<JournalEntryListItem>> grouped = new LinkedHashMap<>();
        for (JournalEntryListItem entry : entries) {
            LocalDate day = entry.getTimestamp().toLocalDate();
            grouped.computeIfAbsent(day, ignored -> new ArrayList<>()).add(entry);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd. MMMM", locale);
        List<JournalDayGroup> dayGroups = new ArrayList<>();
        grouped.forEach((day, items) -> dayGroups.add(new JournalDayGroup(day, day.format(formatter), items)));
        return dayGroups;
    }

    private String formatArchiveLabel(String monthKey, Locale locale) {
        try {
            return YearMonth.parse(monthKey).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale));
        } catch (Exception ignored) {
            return monthKey;
        }
    }

    private String writeEntryData(JournalEntryData entryData) {
        try {
            return objectMapper.writeValueAsString(entryData);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    public static class ArchiveMonthGroup {
        private final String key;
        private final String label;
        private final List<JournalEntry> entries;

        public ArchiveMonthGroup(String key, String label, List<JournalEntry> entries) {
            this.key = key;
            this.label = label;
            this.entries = entries;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public List<JournalEntry> getEntries() {
            return entries;
        }
    }
}
