package com.digitallife.journal_site.communities;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * service class to interact with the database, see below to see current uses
 */
@Service
public class CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * will create a new community
     *
     * @param communityName name of the new community
     * @param description description of the new community
     * @param username the user who created the new community
     * @return the newly created community
     */
    public Community createCommunity(String communityName,String description, String username) {
        Community community = new Community();
        community.setName(communityName);
        community.setDescription(description);

        // Save the community to the database
        Community savedCommunity = communityRepository.save(community);

        // Optionally, add the creator to the community
        Optional<User> userOptional = userRepository.findByUsername(username);
        userOptional.ifPresent(user -> {
            savedCommunity.getUsers().add(user);
            user.getCommunities().add(savedCommunity);
            userRepository.save(user); // Save the updated user
        });

        return savedCommunity;
    }

    /**
     * will add a user to a community
     *
     * @param communityId the id which the user needs to be added to
     * @param username the username of the user which needs to be added to a community
     * @return the community to which the user is added
     */
    public Community addUserToCommunity(Long communityId, String username) {
        Optional<Community> communityOptional = communityRepository.findById(communityId);
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (communityOptional.isPresent() && userOptional.isPresent()) {
            Community community = communityOptional.get();
            User user = userOptional.get();

            //check whether a user is already part of a community
            if (!community.getUsers().contains(user)) {
                // Add the user to the community
                community.getUsers().add(user);
                user.getCommunities().add(community);

                // Save the changes to the database
                communityRepository.save(community);
                userRepository.save(user);
            }

            return community;
        } else {
            // Handle cases where community or user does not exist
            throw new RuntimeException("Community or User not found");
        }
    }

    /**
     * finds the communities of the current user
     * TODO limit the number of communities passed and implement a scroll and search function in the html page (better for performance in case of large database)
     *
     * @param username username of the user
     * @return set of all communities which the user is a part of
     */
    public Set<Community> findCommunityByUsername(String username) {
        return communityRepository.findByUsername(username);
    }

    /**
     * finds all communities in the database
     * TODO limit the number of communities passed and implement a scroll and search function in the html page (better for performance in case of large database)
     *
     * @return all communities in the database
     */
    public List<Community> findAll() {
        return communityRepository.findAll();
    }

    public Community findByID(Long communityId) {
        return communityRepository.findById(communityId).orElseThrow(() -> new RuntimeException("Community not found"));
    }
}

