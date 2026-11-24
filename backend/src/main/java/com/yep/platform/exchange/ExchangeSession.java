package com.yep.platform.exchange;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "exchange_sessions")
public class ExchangeSession extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String title, description, topic;
    @Column(name = "starts_at") public Instant startsAt;
    @Column(name = "duration_minutes") public int durationMinutes = 60;
    @Column(name = "facilitator_id") public Long facilitatorId;
    @Column(name = "meeting_link") public String meetingLink;
    @Enumerated(EnumType.STRING) public ExchangeStatus status = ExchangeStatus.SCHEDULED;
    @Column(name = "discussion_prompts") public String discussionPrompts;
    public String resources;
    public ExchangeSession() {}
}
