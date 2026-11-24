package com.yep.platform.team;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "team_invitations")
public class TeamInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "team_id") public Long teamId;
    public String email;
    @Column(name = "invited_by") public Long invitedBy;
    public String status = "PENDING";
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public TeamInvitation() {}
}
