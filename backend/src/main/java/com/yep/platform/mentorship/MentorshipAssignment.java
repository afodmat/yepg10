package com.yep.platform.mentorship;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "mentorship_assignments")
public class MentorshipAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "mentor_id") public Long mentorId;
    @Column(name = "participant_id") public Long participantId;
    @Column(name = "team_id") public Long teamId;
    public String status = "ACTIVE";
    @Column(name = "assigned_by") public Long assignedBy;
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public MentorshipAssignment() {}
}
