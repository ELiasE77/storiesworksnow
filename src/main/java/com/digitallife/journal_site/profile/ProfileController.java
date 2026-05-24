package com.digitallife.journal_site.profile;

import com.digitallife.journal_site.Journal.JournalEntryListItem;
import com.digitallife.journal_site.Journal.JournalInsightItem;
import com.digitallife.journal_site.Journal.JournalInsightsService;
import com.digitallife.journal_site.Journal.JournalService;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final UserDetailService userDetailService;
    private final PersonaService personaService;
    private final JournalService journalService;
    private final JournalInsightsService journalInsightsService;

    public ProfileController(ProfileRepository profileRepo,
                             UserDetailService uds,
                             PersonaService personaService,
                             JournalService journalService,
                             JournalInsightsService journalInsightsService) {
        this.profileRepo       = profileRepo;
        this.userDetailService = uds;
        this.personaService    = personaService;
        this.journalService = journalService;
        this.journalInsightsService = journalInsightsService;
    }

    /**
     * Resolve the current user's id. Falls back to the security context if the
     * session attribute is missing.
     */
    private Long getCurrentUserId(HttpSession session) {
        Long id = (Long) session.getAttribute("currentUserId");
        if (id != null) {
            return id;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            User user = userDetailService.findByUsername(auth.getName());
            if (user != null) {
                session.setAttribute("currentUserId", user.getId());
                return user.getId();
            }
        }
        return null;
    }

    @GetMapping("/questionnaire")
    public String showQuestionnaire(HttpSession session, Model model) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        // Load existing or new blank
        Profile existing = profileRepo
                .findByUserId(userId)
                .orElse(new Profile());
        model.addAttribute("profile", existing);
        model.addAttribute("companionOptions", CompanionCatalog.all());
        return "questionnaire";
    }

    @PostMapping("/questionnaire")
    public String submitQuestionnaire(
            @ModelAttribute("profile") Profile formData,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            HttpSession session
    ) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        // Lookup the current user
        User user = userDetailService.findById(userId);

        // Load or create the persistent Profile
        Profile p = profileRepo
                .findByUserId(userId)
                .orElseGet(Profile::new);

        // Associate the user (MapsId will populate the PK)
        p.setUser(user);

        // Copy in all questionnaire fields
        p.setName(formData.getName());
        p.setGender(formData.getGender());
        p.setAge(formData.getAge());
        p.setHeight(formData.getHeight());
        p.setNationality(formData.getNationality());
        p.setHair(formData.getHair());
        p.setHobbies(formData.getHobbies());
        p.setPersona(formData.getPersona());
        p.setCompanionKey(CompanionCatalog.find(formData.getCompanionKey()).key());

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                java.nio.file.Path dir = java.nio.file.Paths.get("uploads").toAbsolutePath();
                java.nio.file.Files.createDirectories(dir);
                String original = java.nio.file.Paths.get(profileImage.getOriginalFilename()).getFileName().toString();
                String filename = "profile_" + userId + "_" + original;
                java.nio.file.Path path = dir.resolve(filename);
                profileImage.transferTo(path.toFile());
                p.setImageUrl("/uploads/" + filename);
                p.setAppearanceJson(personaService.analyseImage(path, userId));
            } catch (Exception e) {
                p.setImageUrl(null);
            }
        }

        // Generate persona feature using OpenAI
        try {
            String feature = personaService.generatePersonaFeature(p);
            p.setPersonaFeature(feature);
        } catch (Exception e) {
            // fall back to empty text if generation fails
            p.setPersonaFeature("");
        }

        // Persist
        profileRepo.saveAndFlush(p);

        // Redirect to feature editing page so user can adjust the generated text
        return "redirect:/profile/feature";
    }

    /** Show the persona feature edit form for the logged-in user. */
    @GetMapping("/feature")
    public String editFeatureForm(HttpSession session, Model model) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        Profile profile = profileRepo.findByUserId(userId).orElse(null);
        if (profile == null) {
            return "redirect:/profile/questionnaire";
        }

        model.addAttribute("profile", profile);
        model.addAttribute("currentUsername", userDetailService.findById(userId).getUsername());
        model.addAttribute("companionOptions", CompanionCatalog.all());
        return "profile/editFeature";
    }

    /** Persist edits to the persona feature text. */
    @PostMapping("/feature")
    public String saveFeature(
            @ModelAttribute("profile") Profile form,
            HttpSession session
    ) {
        Long userId = getCurrentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        Profile profile = profileRepo.findByUserId(userId).orElse(null);
        if (profile == null) {
            return "redirect:/profile/questionnaire";
        }

        profile.setPersonaFeature(form.getPersonaFeature());
        profileRepo.saveAndFlush(profile);

        return "redirect:/profile/" + profile.getUser().getUsername();
    }

    @GetMapping(
            path = "/{username:^(?!questionnaire$)[a-zA-Z0-9_]+}"
    )
    public String viewProfile(
            @PathVariable String username,
            HttpSession session,
            Model model,
            Locale locale
    ) {
        // Fetch profile if it exists
        Profile profile = profileRepo
                .findByUserUsername(username)
                .orElse(null);

        // Get current user (if logged in)
        Long currentId = getCurrentUserId(session);
        User current = currentId == null
                ? null
                : userDetailService.findById(currentId);

        // If it's your own page but you haven't filled your profile → force questionnaire
        if (current != null
                && current.getUsername().equals(username)
                && profile == null) {
            return "redirect:/profile/questionnaire";
        }

        // Otherwise render
        User user = userDetailService.findByUsername(username);
        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("currentUsername",
                current == null ? "" : current.getUsername());

        // Follow-button logic: check in-memory following set
        boolean isFollowing = current != null
                && current.getFollowing().stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
        model.addAttribute("isFollowing", isFollowing);
        List<JournalInsightItem> insights = journalService.findInsightItems(user);
        List<LocalDateTime> timestamps = insights.stream()
                .map(JournalInsightItem::getTimestamp)
                .toList();
        List<JournalEntryListItem> latestEntries = journalService.findRecentEntryItems(user, PageRequest.of(0, 3));

        model.addAttribute("latestEntries", latestEntries);
        model.addAttribute("entryCount", journalService.countEntriesByUser(user));
        model.addAttribute("weeklyActivity", toWeekCards(journalInsightsService.buildWeeklyActivityFromTimestamps(timestamps), locale));
        model.addAttribute("focusScores", journalInsightsService.buildFocusScoresFromInsights(insights));
        model.addAttribute("streakCount", journalInsightsService.calculateCurrentStreakFromTimestamps(timestamps));
        model.addAttribute("weeklyReflections", journalInsightsService.countTimestampsThisWeek(timestamps));
        model.addAttribute("totalWords", journalInsightsService.countWordsFromInsights(insights));
        model.addAttribute("totalImages", journalInsightsService.countImagesFromInsights(insights));
        model.addAttribute("weeklySentiment", journalInsightsService.buildWeeklySentimentFromInsights(insights, locale));
        model.addAttribute("activeCompanion", CompanionCatalog.find(profile == null ? null : profile.getCompanionKey()));
        model.addAttribute("companionOptions", CompanionCatalog.all());

        return "user/userProfile";
    }

    private List<Map<String, Object>> toWeekCards(Map<DayOfWeek, Integer> weeklyActivity, Locale locale) {
        List<Map<String, Object>> cards = new ArrayList<>();
        DayOfWeek[] orderedDays = {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        };

        for (DayOfWeek dayOfWeek : orderedDays) {
            cards.add(Map.of(
                    "label", dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replace(".", ""),
                    "count", weeklyActivity.getOrDefault(dayOfWeek, 0)
            ));
        }
        return cards;
    }
}
