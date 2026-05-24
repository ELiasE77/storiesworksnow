package com.digitallife.journal_site.Security;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalInsightsService;
import com.digitallife.journal_site.Journal.JournalService;
import com.digitallife.journal_site.profile.CompanionCatalog;
import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class PageController {

    private final JournalService journalService;
    private final JournalInsightsService journalInsightsService;
    private final UserDetailService userDetailService;
    private final ProfileRepository profileRepository;

    public PageController(JournalService journalService,
                          JournalInsightsService journalInsightsService,
                          UserDetailService userDetailService,
                          ProfileRepository profileRepository) {
        this.journalService = journalService;
        this.journalInsightsService = journalInsightsService;
        this.userDetailService = userDetailService;
        this.profileRepository = profileRepository;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal, Locale locale) {
        User user = userDetailService.findByUsername(principal.getName());
        List<LocalDateTime> timestamps = journalService.findEntryTimestamps(user);
        JournalEntry recentEntry = journalService.findMostRecentEntry(user);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        model.addAttribute("recentEntry", recentEntry);
        model.addAttribute("weeklyReflections", journalInsightsService.countTimestampsThisWeek(timestamps));
        model.addAttribute("streakCount", journalInsightsService.calculateCurrentStreakFromTimestamps(timestamps));
        model.addAttribute("currentCompanion", CompanionCatalog.find(profile == null ? null : profile.getCompanionKey()));
        model.addAttribute("weeklyActivity", toWeekCards(journalInsightsService.buildWeeklyActivityFromTimestamps(timestamps), locale));
        return "home";
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
