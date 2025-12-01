package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.ChatGptIntegration.ChatGPTController;
import com.digitallife.journal_site.communities.CommunityService;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class JournalController {

    @Autowired private JournalService      journalService;
    @Autowired private UserDetailService  userService;
    @Autowired private CommunityService   communityService;
    @Autowired private ChatGPTController  chatGptController;
    @Autowired private com.digitallife.journal_site.profile.ProfileRepository profileRepository;
    @Autowired private ImageDescriptionService imageDescriptionService;

    // 1) Show the “new entry” form
    @GetMapping("/journal")
    public String showCreateForm(Model model, Authentication auth) {
        model.addAttribute("communities",
                communityService.findCommunityByUsername(auth.getName()));
        return "journaling/journal";
    }

    // 2) JSON‐based save endpoint
    @PostMapping(
            value    = "/api/journal/save",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<String> saveViaJson(
            @RequestBody JournalEntryDTO dto,
            Authentication auth
    ) throws Exception {
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity
                    .badRequest()
                    .body("{\"error\":\"User not found: "+ auth.getName() +"\"}");
        }

        if (dto.getVisibility() == null) {
            dto.setVisibility(JournalEntry.Visibility.PRIVATE);
        }

        journalService.saveJournalEntry(
                user,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageUrl(),   // raw base64 only
                dto.getSceneDescription(),
                dto.getCommunityId(),
                dto.getVisibility()
        );

        JSONObject resp = new JSONObject()
                .put("status", "ok")
                .put("redirectUrl", "/journal/home");        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp.toString());
    }

    // 3) List “your entries” (filtered by month/year for faster load)
    @GetMapping("/journal/home")
    public String showUserJournals(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model,
            Principal principal
    ) {        User user = userService.findByUsername(principal.getName());
        List<JournalEntrySummary> entries = journalService.getEntrySummariesByUserAndMonth(user, month, year);

        LocalDate now = LocalDate.now();
        int selectedMonth = (month == null || month < 1 || month > 12) ? now.getMonthValue() : month;
        int selectedYear  = (year  == null || year  < 1) ? now.getYear()       : year;

        model.addAttribute("entries", entries);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("monthYearOptions", journalService.findDistinctMonthsAndYears(user));
        return "user/UserJournalEntries";
    }

    // 4) Public / social feed
    @GetMapping("/journal/sharePage")
    public String getAllJournalEntries(Model model) {
        List<JournalEntry> entries = journalService.findPublicEntriesSortedByTimestamp();
        model.addAttribute("entries", entries);
        return "social/social_home";
    }

    // 5) Timeline (month/year filter)
    @GetMapping("/journal/timeline")
    public String showImageTimeline(
            @RequestParam(value="month", required=false) Integer month,
            @RequestParam(value="year",  required=false) Integer year,
            Model model,
            Principal principal
    ) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("availableMonthsAndYears",
                journalService.findMonthAndYear(user));

        LocalDateTime now = LocalDateTime.now();
        int selMonth = (month == null) ? now.getMonthValue() : month;
        int selYear  = (year  == null) ? now.getYear()       : year;

        List<JournalEntry> entries =
                journalService.findEntriesForMonthAndYear(user, selMonth, selYear);

        model.addAttribute("selectedMonth", selMonth);
        model.addAttribute("selectedYear",  selYear);
        model.addAttribute("entries",       entries);
        return "journaling/timeline";
    }

    // 6) Edit form
    @GetMapping("/journal/edit")
    public String editJournalEntry(
            @RequestParam("id") Long id,
            Model model,
            Authentication auth
    ) {
        model.addAttribute("entry",
                journalService.findJournalEntryById(id));
        model.addAttribute("communities",
                communityService.findCommunityByUsername(auth.getName()));
        profileRepository.findByUserUsername(auth.getName())
                .ifPresent(p -> model.addAttribute("personaFeature", p.getPersonaFeature()));
        return "journaling/journal_editing";
    }

    // 7) Update (form‐submit)
    @PostMapping("/journal/update")
    public String updateJournalEntry(
            @RequestParam("id") Long id,
            @RequestParam(value="title", required = false) String title,
            @RequestParam("content") String content,
            @RequestParam(value="imageUrl", required=false) String imageUrl,
            @RequestParam(value="sceneDescription", required=false) String sceneDescription,
            @RequestParam("visibility") JournalEntry.Visibility visibility,
            @RequestParam(value="communityId", required=false) String communityIdStr
    ) {
        // Strip data URL prefix if present
        if (imageUrl != null && imageUrl.startsWith("data:image")) {
            int comma = imageUrl.indexOf(',');
            if (comma > 0) {
                imageUrl = imageUrl.substring(comma + 1);
            }
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            sceneDescription = null;
        }


        // Parse communityId safely
        Long communityId = null;
        if (communityIdStr != null && !communityIdStr.isBlank()) {
            try {
                communityId = Long.valueOf(communityIdStr);
            } catch (NumberFormatException ignore) {
                // leave null if invalid
            }
        }

        journalService.updateJournalEntry(
                id, title, content, imageUrl, sceneDescription, visibility, communityId
        );
        return "redirect:/journal/home";
    }

    @PostMapping(value = "/api/journal/analyze-image", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> analyzeImage(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String imageBase64 = payload.get("imageBase64");
        if (imageBase64 == null || imageBase64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No image provided"));
        }

        return imageDescriptionService.describeScene(imageBase64)
                .map(desc -> ResponseEntity.ok(Map.of("sceneDescription", desc)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("sceneDescription", "")));
    }

    // 8) Delete
    @PostMapping("/journal/delete")
    public String deleteJournalEntry(@RequestParam("id") Long id) {
        journalService.deleteJournalEntry(id);
        return "redirect:/journal/home";
    }

    @GetMapping(value = "/api/journal/{id}/image", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> getJournalEntryImage(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JournalEntry entry = journalService.findJournalEntryById(id);
            boolean isOwner = entry.getUser().getUsername().equals(principal.getName());
            if (!isOwner && entry.getVisibility() == JournalEntry.Visibility.PRIVATE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return journalService.getImageForEntry(id)
                    .map(image -> ResponseEntity.ok(Map.of("imageUrl", image)))
                    .orElseGet(() -> ResponseEntity.ok(Map.of("imageUrl", "")));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/api/journal/entries", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<JournalEntrySummary>> getCurrentUserEntries(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<JournalEntrySummary> entries = journalService.getEntrySummariesByUserAndMonth(user, month, year);
        return ResponseEntity.ok(entries);
    }
}
