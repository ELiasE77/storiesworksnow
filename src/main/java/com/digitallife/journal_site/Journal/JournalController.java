package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalService journalService;

    @Autowired
    private UserDetailService userService; // Service to fetch user details by username

    // Endpoint to handle saving a journal entry
    @PostMapping("/save")
    public String saveJournalEntry(@RequestParam("content") String content, Authentication authentication) {
        // Get the logged-in user's username from Authentication
        String username = authentication.getName();

        // Fetch the user by username using the UserService (which uses UserRepository)
        User user = userService.findByUsername(username);

        if (user != null) {
            // Save the journal entry for this user
            journalService.saveJournalEntry(user, content);
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
            // Fetch all journal entries for the logged-in user
            List<JournalEntry> journalEntries = journalService.getEntriesByUser(user);

            // Add the entries to the model to be displayed in the HTML page
            model.addAttribute("entries", journalEntries);
        }

        return "journal";
    }
}
