package com.digitallife.journal_site.social;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserDetailService userDetailService;

    public ChatController(ChatService chatService,
                          UserDetailService userDetailService) {
        this.chatService = chatService;
        this.userDetailService = userDetailService;
    }

    @GetMapping
    public String chatHome(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User current = userDetailService.findByUsername(authentication.getName());
        List<User> contacts = chatService.findMutualFollowers(current);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentUser", current);
        return "social/chat";
    }

    @GetMapping("/api/history/{username}")
    @ResponseBody
    public ResponseEntity<List<DirectMessageDto>> loadHistory(@PathVariable String username,
                                                              Authentication authentication) {
        User current = getAuthenticatedUser(authentication);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User target = userDetailService.findByUsername(username);
        if (target == null || !canChat(current, target)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<DirectMessageDto> history = chatService.getConversation(current, target)
                .stream()
                .map(DirectMessageDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/api/send")
    @ResponseBody
    public ResponseEntity<DirectMessageDto> sendMessage(@RequestBody Map<String, String> payload,
                                                        Authentication authentication) {
        User current = getAuthenticatedUser(authentication);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String recipientUsername = payload.get("recipient");
        User recipient = userDetailService.findByUsername(recipientUsername);
        if (recipient == null || !canChat(current, recipient)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String content = payload.getOrDefault("content", "").trim();
        String imageData = payload.getOrDefault("imageData", "").trim();
        if (content.isEmpty() && imageData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        DirectMessage saved = chatService.saveMessage(current, recipient, content, imageData.isEmpty() ? null : imageData);
        return ResponseEntity.ok(DirectMessageDto.from(saved));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userDetailService.findByUsername(authentication.getName());
    }

    private boolean canChat(User current, User target) {
        return current.getFollowing().contains(target) && target.getFollowing().contains(current);
    }

    public record DirectMessageDto(long id,
                                   String sender,
                                   String recipient,
                                   String content,
                                   String imageData,
                                   String timestamp) {
        static DirectMessageDto from(DirectMessage message) {
            return new DirectMessageDto(
                    message.getId(),
                    message.getSender().getUsername(),
                    message.getRecipient().getUsername(),
                    message.getContent(),
                    message.getImageData(),
                    message.getTimestamp().toString()
            );
        }
    }
}
