package com.digitallife.journal_site.social;

import com.digitallife.journal_site.user.User;
import com.digitallife.journal_site.user.UserDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/social")
public class SocialInteractionController {

    private final SocialInteractionService socialInteractionService;
    private final UserDetailService userDetailService;

    public SocialInteractionController(SocialInteractionService socialInteractionService,
                                       UserDetailService userDetailService) {
        this.socialInteractionService = socialInteractionService;
        this.userDetailService = userDetailService;
    }

    @PostMapping("/{entryId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long entryId,
                                                 @RequestBody Map<String, String> payload,
                                                 Authentication authentication) {
        User current = getCurrentUser(authentication);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        EntryComment saved = socialInteractionService.addComment(entryId, current, content);
        return ResponseEntity.ok(CommentDto.from(saved));
    }

    @GetMapping("/{entryId}/comments")
    public ResponseEntity<List<CommentDto>> listComments(@PathVariable Long entryId) {
        List<CommentDto> comments = socialInteractionService.listComments(entryId)
                .stream()
                .map(CommentDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{entryId}/likes/toggle")
    public ResponseEntity<LikeDto> toggleLike(@PathVariable Long entryId,
                                              Authentication authentication) {
        User current = getCurrentUser(authentication);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean liked = socialInteractionService.toggleLike(entryId, current);
        long total = socialInteractionService.countLikes(entryId);
        return ResponseEntity.ok(new LikeDto(liked, total));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userDetailService.findByUsername(authentication.getName());
    }

    public record CommentDto(long id,
                             String author,
                             String content,
                             String timestamp) {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        static CommentDto from(EntryComment comment) {
            return new CommentDto(
                    comment.getId(),
                    comment.getAuthor().getUsername(),
                    comment.getContent(),
                    comment.getTimestamp().format(FORMATTER)
            );
        }
    }

    public record LikeDto(boolean liked, long totalLikes) { }
}