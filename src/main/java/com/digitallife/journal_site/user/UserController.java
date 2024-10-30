package com.digitallife.journal_site.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller used to enter the following pages:
 * profile page (both the page of the current user and others)
 *
 * And used to follow or unfollow A user
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserDetailService userDetailService;


    /**
     * show to profile page of the current user
     * TODO still make a seperate profile for the current user (its using the same one as the other users)
     *
     * @param model used to pass user info the html page
     * @param authentication used to get the name of the current user
     * @return name of the html page
     */
    @GetMapping("/profile")
    public String showProfilePage(Model model, Authentication authentication) {
        String currentUsername = authentication.getName();

        User user = userDetailService.findByUsername(currentUsername);

        model.addAttribute("isFollowing", true);
        model.addAttribute("user", user);
        model.addAttribute("currentUsername", currentUsername);

        return "user/userProfile";
    }

    /**
     * shows the page of other users
     *
     * @param username the username of the user whose page which you want to see
     * @param principal to get the current username (Principal and Authentication can both be used)
     * @param model used to pass information to
     * @return the string of the html page to open in the templates folder
     */
    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username, Principal principal, Model model) {
        // Find logged-in user (the viewer)
        User currentUser = userDetailService.findByUsername(principal.getName());
        String currentUsername = principal.getName();

        // Find the profile user
        User profileUser = userDetailService.findByUsername(username);

        // Check if the current user is already following the profile user
        boolean isFollowing = currentUser.getFollowing().contains(profileUser);

        // Add the necessary data to the model
        model.addAttribute("user", profileUser);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("currentUsername", currentUsername);


        // Return the profile view
        return "user/userProfile";
    }

    /**
     * follow a user
     *
     * @param principal used to get the current username
     * @param username used to get the User entity of the person to follow
     * @return a string which returns the user to the follower's profile
     */
    @PostMapping("/follow/{username}")
    public ResponseEntity<String> followUser(Principal principal, @PathVariable String username) {
        // Find logged-in user (the follower)
        User follower = userDetailService.findByUsername(principal.getName());

        // Find the user to be followed
        User followed = userDetailService.findByUsername(username);

        // Add the user to the follower's following list
        if (!follower.getFollowing().contains(followed)) {
            follower.getFollowing().add(followed);

            userDetailService.saveUser(follower);
            return ResponseEntity.ok("Followed " + username);
        } else {
            return ResponseEntity.badRequest().body("Not following " + username);
        }
    }

    /**
     * unfollow a user
     *
     * @param principal used to get the username of the current user
     * @param username used to find the user entity of the person to unfollow
     * @return a string which returns the user to the follower's profile
     */
    @PostMapping("/unfollow/{username}")
    public ResponseEntity<String> unfollowUser(Principal principal, @PathVariable String username) {
        // Find logged-in user (the follower)
        User follower = userDetailService.findByUsername(principal.getName());

        // Find the user to be unfollowed
        User followed = userDetailService.findByUsername(username);

        // Remove the user from the follower's following list
        if (follower.getFollowing().contains(followed)) {
            follower.getFollowing().remove(followed);
            userDetailService.saveUser(follower);
            return ResponseEntity.ok("Unfollowed " + username);
        } else {
            return ResponseEntity.badRequest().body("Not following " + username);
        }
    }

}
