package com.digitallife.journal_site.social;

import com.digitallife.journal_site.user.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final DirectMessageRepository directMessageRepository;

    public ChatService(DirectMessageRepository directMessageRepository) {
        this.directMessageRepository = directMessageRepository;
    }

    public List<User> findMutualFollowers(User user) {
        if (user == null) {
            return List.of();
        }
        Set<User> following = user.getFollowing();
        return following.stream()
                .filter(other -> other.getFollowing().contains(user))
                .collect(Collectors.toList());
    }

    public List<DirectMessage> getConversation(User current, User target) {
        return directMessageRepository.findConversation(current, target);
    }

    public DirectMessage saveMessage(User sender,
                                     User recipient,
                                     String content,
                                     String imageData) {
        DirectMessage msg = new DirectMessage();
        msg.setSender(sender);
        msg.setRecipient(recipient);
        msg.setContent(content);
        msg.setImageData(imageData);
        msg.setTimestamp(LocalDateTime.now());
        return directMessageRepository.save(msg);
    }
}
