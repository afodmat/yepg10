package com.yep.platform.exchange;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "exchange_participants")
public class ExchangeParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "session_id") public Long sessionId;
    @Column(name = "user_id") public Long userId;
    public String country;
    @Column(name = "registered_at") public Instant registeredAt = Instant.now();
    public ExchangeParticipant() {}
}
