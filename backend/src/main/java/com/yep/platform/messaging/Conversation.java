package com.yep.platform.messaging;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String type;
    @Column(name = "team_id") public Long teamId;
    @Column(name = "direct_key") public String directKey;
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public Conversation() {}
}
