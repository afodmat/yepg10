package com.yep.platform.messaging;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity @Table(name = "conversation_participants") @IdClass(ConversationParticipant.Key.class)
public class ConversationParticipant {
    public static class Key implements Serializable {
        public Long conversationId, userId;
        public Key() {}
        public Key(Long c, Long u) { conversationId = c; userId = u; }
        @Override public boolean equals(Object o) { return o instanceof Key k && Objects.equals(conversationId, k.conversationId) && Objects.equals(userId, k.userId); }
        @Override public int hashCode() { return Objects.hash(conversationId, userId); }
    }
    @Id @Column(name = "conversation_id") public Long conversationId;
    @Id @Column(name = "user_id") public Long userId;
    @Column(name = "last_read_at") public Instant lastReadAt;
    public ConversationParticipant() {}
    public ConversationParticipant(Long c, Long u) { conversationId = c; userId = u; }
}
