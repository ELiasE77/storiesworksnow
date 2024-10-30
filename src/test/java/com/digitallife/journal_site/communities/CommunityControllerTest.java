package com.digitallife.journal_site.communities;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalService;
import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@WebMvcTest(CommunityController.class)
@AutoConfigureMockMvc(addFilters = false)  // Disable security filters for this test
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc; // Ignore this error! This is error is not correct and testing will work

    @MockBean
    private UserDetailService userDetailService; // Mock the UserDetailService

    @MockBean
    private  CommunityService communityService; // Mock the CommunityService

    @MockBean
    private JournalService journalService; // Mock the journalService

    @Mock
    private Authentication authentication; // Mock the authentication

    private User mockUser;
    private Community mockCommunity;
    private Set<Community> mockSet = new HashSet<>();
    private List<Community> mockList = new ArrayList<>();
    private JournalEntry mockEntries;

    @BeforeEach
    public void setup() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Mock the authentication to return a username
        when(authentication.getName()).thenReturn("testuser");

        // Create a mock user object
        mockUser = new User();
        mockUser.setUsername("testuser");

        // Mock the userDetailService to return the mock user when called
        when(userDetailService.findByUsername("testuser")).thenReturn(mockUser);

        // create new mock community for testing
        mockCommunity = new Community();
        mockCommunity.setId(1L);
        mockCommunity.setName("testCommunity");

        // add community to list and set for model testing
        mockSet.add(mockCommunity);
        mockList.add(mockCommunity);

        // add this community to the testuser
        mockUser.getCommunities().add(mockCommunity);

        when(communityService.findCommunityByUsername("testuser")).thenReturn(mockSet);
    }

    @Test
    void showYourCommunityPage() throws Exception {
        mockMvc.perform(get("/communities/user").principal(authentication))
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("community/yourCommunities")) // Check that the correct view is returned
                .andExpect(model().attribute("communities", mockSet)); // Check the model attributes passed in the method
    }

    @Test
    void showCommunityOverview() throws Exception {
        // return the mocklist when the communityService is called
        when(communityService.findAll()).thenReturn(mockList);

        mockMvc.perform(get("/communities/communityOverview").principal(authentication))
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("community/allCommunities")) // Check that the correct view is returned
                .andExpect(model().attribute("communities", mockList)); // Check the model attributes passed in the method
    }

    // test getting to the createCommunity page
    @Test
    void showCommunityCreator() throws Exception {
        mockMvc.perform(get("/communities/createCommunity").principal(authentication))
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("community/communityCreator")); // Check that the correct view is returned
    }

    @Test
    void showCommunityPage() throws Exception {
        // Create a mock entry for a journal entry
        mockEntries = new JournalEntry();
        mockEntries.setCommunity(mockCommunity);
        mockEntries.setUser(mockUser);

        // Add the entry to a list to be added to the model
        List<JournalEntry> mockJournalList = new ArrayList<>();
        mockJournalList.add(mockEntries);

        //make sure the methods used in the controller return the right results
        when(journalService.findCommunityEntries(mockCommunity)).thenReturn(mockJournalList);
        when(communityService.findByID(1L)).thenReturn(mockCommunity);

        mockMvc.perform(get("/communities/1").principal(authentication)) // Try to enter the community page
                .andExpect(status().isOk()) // Check that the status is 200 OK
                .andExpect(view().name("community/community")) // Check that the correct view is returned
                .andExpect(model().attribute("community", mockCommunity)) // Check the model attributes passed in the method
                .andExpect(model().attribute("entries", mockJournalList)); // Check the model attributes passed in the method
    }

    @Test
    void createCommunity() throws Exception {

        // Define the community name and description
        String communityName = "Test Community";
        String description = "This is a test community";

        // Perform the POST request to /create with the necessary parameters
        mockMvc.perform(post("/communities/create")
                        .param("communityName", communityName)
                        .param("description", description)
                        .principal(authentication))  // Set the mock Authentication
                .andExpect(status().is3xxRedirection())  // Expect a 3xx redirect status
                .andExpect(redirectedUrl("/communities/user"));  // Check if redirect to /communities/user

        // Verify that the communityService.createCommunity() was called with correct arguments
        verify(communityService).createCommunity(communityName, description, "testuser");

        //test with bad entry data
        mockMvc.perform(post("/communities/create"))
                .andExpect(status().isBadRequest());  // Expect a 400 Bad Request if parameters are missing
    }

    @Test
    void joinCommunity() throws Exception {

        // Perform the POST request to /create with the necessary parameters
        mockMvc.perform(post("/communities/join/1")
                        .principal(authentication))  // Set the mock Authentication
                .andExpect(status().is3xxRedirection())  // Expect a 3xx redirect status
                .andExpect(redirectedUrl("/communities/user"));  // Check if redirect to /communities/user

        // Verify that the communityService.addUserToCommunity() was called with correct arguments
        verify(communityService).addUserToCommunity(1L, "testuser");

    }
}