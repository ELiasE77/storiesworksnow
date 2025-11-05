package com.digitallife.journal_site.social;

import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("""
        SELECT m
          FROM DirectMessage m
         WHERE (m.sender = :userA AND m.recipient = :userB)
            OR (m.sender = :userB AND m.recipient = :userA)
      ORDER BY m.timestamp ASC
    """)
    List<DirectMessage> findConversation(@Param("userA") User userA,
                                         @Param("userB") User userB);
}

