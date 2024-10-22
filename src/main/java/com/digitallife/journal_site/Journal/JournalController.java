package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalService journalService;

    @Autowired
    private UserDetailService userService; // Service to fetch user details by username

    @Autowired
    private CommunityRepository communityRepository;

    // Endpoint to handle saving a journal entry
    @PostMapping("/save")
    public String saveJournalEntry(@RequestParam("content") String content, @RequestParam(value = "imageUrl",
            required = false) String base64Image, @RequestParam("title") String title, @RequestParam(value = "communityId",required = false) Long communityId,
                                   @RequestParam("visibility") JournalEntry.Visibility visibility, Authentication authentication) {
        // Get the logged-in user's username from Authentication
        String username = authentication.getName();

        // Fetch the user by username using the UserService (which uses UserRepository)
        User user = userService.findByUsername(username);

        if (user != null) {
            // Save the journal entry for this user
            journalService.saveJournalEntry(user, title, content, base64Image, communityId, visibility);
        } else {
            // Handle the case where the user is not found (optional)
            throw new RuntimeException("User not found: " + username);
        }

        // Redirect back to the journal page after saving the entry
        return "redirect:/journal";
    }

    // Endpoint to retrieve and display all journal entries for the logged-in user
    @GetMapping
    public String getJournalEntries(Model model, Authentication authentication) {
        // Get the logged-in user's username from Authentication
        String username = authentication.getName();

        // Fetch the user by username using the UserService
        User user = userService.findByUsername(username);

        if (user != null) {
            // Fetch all journal entries and communities for the logged-in user
            List<JournalEntry> journalEntries = journalService.getEntriesByUser(user);
            Set<Community> communities = communityRepository.findByUsername(username);

            // Add the entries and communities to the model to be displayed in the HTML page
            model.addAttribute("entries", journalEntries);
            model.addAttribute("communities", communities);
        }
        return "journaling/journal";
    }

    @GetMapping("/home")
    public String showUserJournals(Model model, Authentication authentication) {
        // Get the logged-in user's username from Authentication
        String username = authentication.getName();

        User user = userService.findByUsername(username);

        if (user != null) {
            List<JournalEntry> journalEntries = journalService.getEntriesByUser(user);

            model.addAttribute("entries", journalEntries);
        }

        return "user/UserJournalEntries";
    }

    // Method to display the edit page from the journal entry page
    @GetMapping("/edit")
    public String editJournalEntry(@RequestParam("id") Long id, Model model) throws ResourceNotFoundException {
        JournalEntry entry = journalService.findJournalEntryById(id);

        model.addAttribute("entry", entry);
        return "journaling/journal_editing";
    }

    // Method to handle the update request from the journal_editing page
    @PostMapping("/update")
    public String updateJournalEntry(@RequestParam("id") Long id,
                                     @RequestParam("content") String content,
                                     @RequestParam("title") String title,
                                     @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                     @RequestParam("visibility") JournalEntry.Visibility visibility,
                                     @RequestParam(value = "community", required = false) Long communityId) {
        journalService.updateJournalEntry(id, title, content, imageUrl, visibility, communityId); // Implement this method
        return "redirect:/journal"; // Redirect back to journal page after updating
    }

    @GetMapping("/sharePage")
    public String getAllJournalEntries(Model model) {
        List<JournalEntry> entries = journalService.findAllEntriesSortedByTimestamp();

        model.addAttribute("entries", entries);
        return "social/social_home"; // Corresponds to the Thymeleaf template
    }

}
