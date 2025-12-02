package com.digitallife.journal_site.social;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.Journal.JournalEntryRepository;
import com.digitallife.journal_site.user.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SocialInteractionService {

    private final JournalEntryRepository journalEntryRepository;
    private final EntryCommentRepository commentRepository;
    private final EntryLikeRepository likeRepository;

    public SocialInteractionService(JournalEntryRepository journalEntryRepository,
                                    EntryCommentRepository commentRepository,
                                    EntryLikeRepository likeRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    public EntryComment addComment(Long entryId, User author, String content) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));

        EntryComment comment = new EntryComment();
        comment.setEntry(entry);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        comment.setTimestamp(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public List<EntryComment> listComments(Long entryId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        return commentRepository.findByEntryOrderByTimestampAsc(entry);
    }

    public boolean toggleLike(Long entryId, User user) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));

        return likeRepository.findByEntryAndUser(entry, user)
                .map(existing -> {
                    likeRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    EntryLike like = new EntryLike();
                    like.setEntry(entry);
                    like.setUser(user);
                    like.setTimestamp(LocalDateTime.now());
                    likeRepository.save(like);
                    return true;
                });
    }

    public long countLikes(Long entryId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
        return likeRepository.countByEntry(entry);
    }
}