package com.digitallife.journal_site.user;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import com.digitallife.journal_site.profile.Profile;
import com.digitallife.journal_site.profile.ProfileRepository;


import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // Ignore this error! This is error is not correct and testing will work

    @MockBean
    private UserDetailService userDetailService; // Mock the UserDetailService

    @MockBean
    private ProfileRepository profileRepo;

    @Mock
    private Authentication authentication; // Mock the authentication

    @Mock
    private Principal principal; // mock the principal

    @BeforeEach
    public void setup() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

    }

    private User follower;
    private User followed;


    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void showProfilePage() throws Exception {
        // Mock the authentication to return a username
        when(authentication.getName()).thenReturn("testuser");

        // Create a mock user object
        User mockUser = new User();
        mockUser.setUsername("testuser");

        Profile mockProfile = new Profile();
        when(profileRepo.findByUserUsername("testuser"))
                .thenReturn(Optional.of(mockProfile));


        // Mock the userDetailService to return the mock user when called
        when(userDetailService.findByUsername("testuser")).thenReturn(mockUser);

        // Perform a GET request to /profile
        mockMvc.perform(get("/user/profile").principal(authentication))
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("user/userProfile")) // Check that the correct view is returned
                .andExpect(model().attribute("isFollowing", true)) // Check the model attributes passed in the method
                .andExpect(model().attribute("user", mockUser)) // Check that the user is added to the model
                .andExpect(model().attribute("currentUsername", "testuser")); // Check that the user is added to the model
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void viewProfile() throws Exception {
        // Mock the authentication to return a username
        when(authentication.getName()).thenReturn("testuser");

        // Create a mock user object
        User mockUser = new User();
        mockUser.setUsername("testuser");

        Profile mockProfile = new Profile();
        when(profileRepo.findByUserUsername("testuser"))
                .thenReturn(Optional.of(mockProfile));


        // Mock the userDetailService to return the mock user when called
        when(userDetailService.findByUsername("testuser")).thenReturn(mockUser);

        mockMvc.perform(get("/user/testuser").principal(authentication))
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("user/userProfile")) // Check that the correct view is returned
                .andExpect(model().attribute("isFollowing", false)) // Check the model attributes passed in the method
                .andExpect(model().attribute("user", mockUser)) // Check that the user is added to the model
                .andExpect(model().attribute("currentUsername", "testuser")); // Check that the user is added to the model
    }

    @Test
    void followUser() throws Exception {
        follower = new User();
        follower.setUsername("follower");

        followed = new User();
        followed.setUsername("followed");

        when(principal.getName()).thenReturn("follower");

        // Mock the userDetailService to return the user to be followed when called
        when(userDetailService.findByUsername("followed")).thenReturn(followed);
    }

    @Test
    void unfollowUser() {
    }
}