package com.digitallife.journal_site.communities;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private UserRepository userRepository;

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

    public Community addUserToCommunity(Long communityId, String username) {
        Optional<Community> communityOptional = communityRepository.findById(communityId);
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (communityOptional.isPresent() && userOptional.isPresent()) {
            Community community = communityOptional.get();
            User user = userOptional.get();

            System.out.println(user);
            System.out.println(community);

            // Add the user to the community
            community.getUsers().add(user);
            user.getCommunities().add(community);

            // Save the changes to the database
            communityRepository.save(community);
            userRepository.save(user);

            return community;
        } else {
            // Handle cases where community or user does not exist
            throw new RuntimeException("Community or User not found");
        }
    }
}

