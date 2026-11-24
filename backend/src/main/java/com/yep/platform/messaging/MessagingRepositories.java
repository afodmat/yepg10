package com.yep.platform.messaging;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public final class MessagingRepositories {
    private MessagingRepositories() {}
    public interface ConversationRepository extends JpaRepository<Conversation, Long> {
        Optional<Conversation> findByDirectKey(String key);
        Optional<Conversation> findByTeamId(Long teamId);
    }
    public interface ParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipant.Key> {
        List<ConversationParticipant> findByUserId(Long userId);
        List<ConversationParticipant> findByConversationId(Long conversationId);
    }
    public interface MessageRepository extends JpaRepository<Message, Long> {
        List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable p);
        List<Message> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long after, Pageable p);
        @Query("select count(m) from Message m where m.conversationId = :cid and m.senderId <> :uid and m.createdAt > :since")
        long unread(@Param("cid") Long conversationId, @Param("uid") Long userId, @Param("since") Instant since);
    }
}
