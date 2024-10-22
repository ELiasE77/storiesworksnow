package com.digitallife.journal_site.communities;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalEntryRepository;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;


@Controller
@RequestMapping("/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    //shows a page with only the journal entries of the user
    @GetMapping("/user")
    public String showYourCommunityPage(Model model, Authentication authentication) {
        String username = authentication.getName();

        // Fetch the user by query from the join table
        Set<Community> communities = communityRepository.findByUsername(username);

        //add them to the model, so they can be accessed on the front end
        model.addAttribute("communities", communities);

        //show this page in the templates folder
        return "community/yourCommunities";
    }

    //shows the page to create a new community
    @GetMapping("/createCommunity")
    public String showCommunityCreator() {

        //when this url is entered you will automatically go to html page called communityCreator
        return "community/communityCreator";
    }

    @GetMapping("/{communityId}")
    public String showCommunityPage(@PathVariable Long communityId, Model model) {
        // Fetch the community by its ID
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));

        // Fetch the journal entries for this community, ordered by date (most recent first)
        List<JournalEntry> journalEntries = journalEntryRepository.findByCommunityOrderByTimestampDesc(community);

        //testing
        System.out.println(journalEntries);

        // Add community and journal entries to the model
        model.addAttribute("community", community);
        model.addAttribute("entries", journalEntries);

        // Return the community page view
        return "/community/community";
    }

    //function is used to create new community when info is sent to /communities/create using HTTP post
    @PostMapping("/create")
    public String createCommunity(@RequestParam("communityName") String communityName,
                                                     @RequestParam("description") String description,
                                                     Authentication authentication) {
        String username = authentication.getName();
        communityService.createCommunity(communityName, description, username);

        //redirect to /communities/user so that the function showCommunityPage is called again, loading the page correctly
        return "redirect:/communities/user";
    }

    //function is used to join a community
    @PostMapping("/join/{communityId}")
    public String joinCommunity(@PathVariable Long communityId, Authentication authentication) {
        String username = authentication.getName();
        communityService.addUserToCommunity(communityId, username);
        return "redirect:/communities/user";
    }
}

