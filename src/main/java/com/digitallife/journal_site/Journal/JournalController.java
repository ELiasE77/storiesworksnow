package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.communities.CommunityRepository;
import com.digitallife.journal_site.communities.CommunityService;
import com.digitallife.journal_site.exceptions.ResourceNotFoundException;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * This controller is used to handle endpoints. HTTP post is handled through here as well as several pages.
 *
 * The pages handled in this class are:
 * Journal Creation page, journal editing page, page which shows all the users journals (UserJournalEntries), public journal entry page, user timeline.
 *
 * The Post methods handled in this class are:
 * saving journal entries ("/save"), editing journal entries ("/update")
 */
@Controller
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalService journalService;

    @Autowired
    private UserDetailService userService; // Service to fetch user details by username

    @Autowired
    private CommunityService communityService;

    /**
     * used to save journal entries in the database (journalEntryRepository) when something is posted to "/journal/save"
     * TODO add security so that not just anyone can send info to this post endpoint
     *
     * @param content the text written in the journal entry
     * @param base64Image image as a base64 String
     * @param title title of the journal entry
     * @param communityId possible ID of the community which the entry needs to be added to
     * @param visibility enumeration of who can see the page (public, private or in a community)
     * @param authentication used to find the name of the current user
     * @return string which says to redirect to the /journal endpoint which is the getJournalEntries function (loads journal html page)
     */
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

    /**
     * Used to enter page when the user fills in the url "/journal" with the communities of the current user
     *
     * @param model to pass information to the html page under the name communities (spring boot functionality)
     * @param authentication to check the name of the current user (spring boot functionality)
     * @return the string of the page in the templates folder
     */
    @GetMapping
    public String getJournalEntries(Model model, Authentication authentication) {
        // Get the logged-in user's username from Authentication
        String username = authentication.getName();

        // Fetch the user by username using the UserService
        User user = userService.findByUsername(username);

        if (user != null) {
            // Fetch all journal entries and communities for the logged-in user
            Set<Community> communities = communityService.findCommunityByUsername(username);

            // Add the entries and communities to the model to be displayed in the HTML page
            model.addAttribute("communities", communities);
        }
        return "journaling/journal";
    }

    /**
     * Shows the page with all the entries of the current user by going to the url "/journal/home"
     *
     * @param model used to pass entries of the current user to the html page (spring boot functionality)
     * @param authentication used to find the current user (spring boot functionality)
     * @return string of the html page (in template folder)
     */
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

    /**
     * used to get to the editing page with all the information of the related journalEntry
     *
     * @param id the id of the journal entry which needs to be edited
     * @param model used to pass info the html page
     * @return string of journal_editing html page in template folder
     */
    @GetMapping("/edit")
    public String editJournalEntry(@RequestParam("id") Long id, Model model, Authentication authentication) {
        String username = authentication.getName();

        JournalEntry entry = journalService.findJournalEntryById(id);

        Set<Community> communities = communityService.findCommunityByUsername(username);

        model.addAttribute("communities", communities);
        model.addAttribute("entry", entry);
        return "journaling/journal_editing";
    }

    /**
     * used to change the data of a journal entry
     *
     * @param id id of journal entry
     * @param content text of the journal entry
     * @param title title of the journal entry
     * @param imageUrl Base64 String of the potential image of journal entry
     * @param visibility enumeration of who can see the journal entry
     * @param communityId id of which community it is potentially shared with
     * @return string which redirects to the journal user page
     */
    @PostMapping("/update")
    public String updateJournalEntry(@RequestParam("id") Long id,
                                     @RequestParam("content") String content,
                                     @RequestParam("title") String title,
                                     @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                     @RequestParam("visibility") JournalEntry.Visibility visibility,
                                     @RequestParam(value = "community", required = false) Long communityId) {
        journalService.updateJournalEntry(id, title, content, imageUrl, visibility, communityId); // change the journal entry
        return "redirect:/journal/home"; // Redirect back to journal user page after updating
    }

    /**
     * used to go to the social page (shows every public journalEntry) entered when using the url ("/journal/sharePage")
     *
     * @param model passes the entries to the public page
     * @return string of the html page which is the public page
     */
    @GetMapping("/sharePage")
    public String getAllJournalEntries(Model model) {
        // Fetch only public entries
        List<JournalEntry> entries = journalService.findPublicEntriesSortedByTimestamp();

        // Add to the model
        model.addAttribute("entries", entries);
        return "social/social_home"; // go to social thymeleaf template
    }

    /**
     * page which shows a timeline containing all the images of the user of a specified month
     *
     * @param month which month does the user want to see
     * @param year which year does the user want to see
     * @param model used to pass info to the html page
     * @param principal used to find the current user
     * @return string of the timeline html page
     */
    @GetMapping("/timeline")
    public String showImageTimeline(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model, Principal principal) {

        // Find the logged-in user
        User user = userService.findByUsername(principal.getName());

        // Fetch distinct months and years for journal entries with images
        List<Object[]> availableMonthsAndYears = journalService.findMonthAndYear(user);
        model.addAttribute("availableMonthsAndYears", availableMonthsAndYears);

        // If no month or year is provided, default to the current month
        LocalDateTime now = LocalDateTime.now();
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();

        // Fetch the journal entries for the user for the selected month and year
        List<JournalEntry> journalEntriesWithImages = journalService.findEntriesForMonthAndYear(user, month, year);
        model.addAttribute("entries", journalEntriesWithImages);

        // Add the selected month and year to the model
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);

        return "journaling/timeline";  // Return the Thymeleaf template for the timeline
    }

    @PostMapping("/delete")
    public String deleteJournalEntry(@RequestParam("id") Long id) {
        journalService.deleteJournalEntry(id);
        return "redirect:/journal/home"; // Redirect back to journal user page after updating
    }

}
