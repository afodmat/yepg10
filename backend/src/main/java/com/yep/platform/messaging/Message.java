package com.yep.platform.messaging;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "conversation_id") public Long conversationId;
    @Column(name = "sender_id") public Long senderId;
    public String body;
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public Message() {}
}
