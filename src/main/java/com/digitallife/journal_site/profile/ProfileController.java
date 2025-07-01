package com.digitallife.journal_site.profile;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileRepository profileRepo;
    private final UserDetailService userDetailService;
    private final PersonaService personaService;

    public ProfileController(ProfileRepository profileRepo,
                             UserDetailService uds,
                             PersonaService personaService) {
        this.profileRepo       = profileRepo;
        this.userDetailService = uds;
        this.personaService    = personaService;
    }

    @GetMapping("/questionnaire")
    public String showQuestionnaire(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("currentUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        // Load existing or new blank
        Profile existing = profileRepo
                .findByUserId(userId)
                .orElse(new Profile());
        model.addAttribute("profile", existing);
        return "questionnaire";
    }

    @PostMapping("/questionnaire")
    public String submitQuestionnaire(
            @ModelAttribute("profile") Profile formData,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("currentUserId");
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
        p.setGender(formData.getGender());
        p.setAge(formData.getAge());
        p.setHeight(formData.getHeight());
        p.setNationality(formData.getNationality());
        p.setHair(formData.getHair());
        p.setHobbies(formData.getHobbies());
        p.setPersona(formData.getPersona());

        // Regenerate persona-feature
        String feature = personaService.generatePersonaFeature(p);
        p.setPersonaFeature(feature);

        // Persist
        profileRepo.save(p);

        // Redirect to their public profile
        return "redirect:/profile/" + user.getUsername();
    }

    @GetMapping(
            path = "/{username:^(?!questionnaire$)[a-zA-Z0-9_]+}"
    )
    public String viewProfile(
            @PathVariable String username,
            HttpSession session,
            Model model
    ) {
        // Fetch profile if it exists
        Profile profile = profileRepo
                .findByUserUsername(username)
                .orElse(null);

        // Get current user (if logged in)
        Long currentId = (Long) session.getAttribute("currentUserId");
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
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("currentUsername",
                current == null ? "" : current.getUsername());

        // Follow-button logic: check in-memory following set
        boolean isFollowing = current != null
                && current.getFollowing().stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
        model.addAttribute("isFollowing", isFollowing);

        return "userProfile";
    }
}
