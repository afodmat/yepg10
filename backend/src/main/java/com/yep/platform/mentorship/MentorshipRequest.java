package com.yep.platform.mentorship;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "mentorship_requests")
public class MentorshipRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "participant_id") public Long participantId;
    @Column(name = "mentor_id") public Long mentorId;
    public String message;
    public String status = "PENDING";
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public MentorshipRequest() {}
}
