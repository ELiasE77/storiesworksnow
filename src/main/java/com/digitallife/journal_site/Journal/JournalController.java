package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.ChatGptIntegration.ChatGPTController;
import com.digitallife.journal_site.communities.CommunityService;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class JournalController {

    @Autowired private JournalService      journalService;
    @Autowired private UserDetailService  userService;
    @Autowired private CommunityService   communityService;
    @Autowired private ChatGPTController  chatGptController;
    @Autowired private com.digitallife.journal_site.profile.ProfileRepository profileRepository;

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
        // find current user
        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            return ResponseEntity
                    .badRequest()
                    .body("{\"error\":\"User not found: "+ auth.getName() +"\"}");
        }

        // default visibility if missing
        if (dto.getVisibility() == null) {
            dto.setVisibility(JournalEntry.Visibility.PRIVATE);
        }

        // you could still do your ChatGPT‐fallback here if dto.getImageUrl() is blank…

        // CALL YOUR existing void method:
        journalService.saveJournalEntry(
                user,
                dto.getTitle(),
                dto.getContent(),
                dto.getImageUrl(),
                dto.getCommunityId(),
                dto.getVisibility()
        );

        // simple success response
        JSONObject resp = new JSONObject()
                .put("status", "ok");
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp.toString());
    }

    // 3) List “your entries”
    @GetMapping("/journal/home")
    public String showUserJournals(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<JournalEntry> entries = journalService.getEntriesByUser(user);
        entries.forEach(e -> {
            String url = e.getImageUrl();
            if (url != null && !url.startsWith("data:")) {
                e.setImageUrl("data:image/png;base64," + url);
            }
        });
        model.addAttribute("entries", entries);
        return "user/UserJournalEntries";
    }

    // 4) Public / social feed
    @GetMapping("/journal/sharePage")
    public String getAllJournalEntries(Model model) {
        List<JournalEntry> entries =
                journalService.findPublicEntriesSortedByTimestamp();
        entries.forEach(e -> {
            String url = e.getImageUrl();
            if (url != null && !url.startsWith("data:")) {
                e.setImageUrl("data:image/png;base64," + url);
            }
        });
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
        entries.forEach(e -> {
            String url = e.getImageUrl();
            if (url != null && !url.startsWith("data:")) {
                e.setImageUrl("data:image/png;base64," + url);
            }
        });

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
            @RequestParam(value="title", required = false) String title,            @RequestParam("content") String content,
            @RequestParam(value="imageUrl",    required=false) String imageUrl,
            @RequestParam("visibility")       JournalEntry.Visibility visibility,
            @RequestParam(value="communityId", required=false) Long communityId
    ) {
        if (imageUrl != null && !imageUrl.startsWith("data:")) {
            imageUrl = "data:image/png;base64," + imageUrl;
        }
        journalService.updateJournalEntry(
                id, title, content, imageUrl, visibility, communityId
        );
        return "redirect:/journal/home";
    }

    // 8) Delete
    @PostMapping("/journal/delete")
    public String deleteJournalEntry(@RequestParam("id") Long id) {
        journalService.deleteJournalEntry(id);
        return "redirect:/journal/home";
    }
}
