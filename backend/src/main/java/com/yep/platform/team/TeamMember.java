package com.yep.platform.team;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "team_members")
public class TeamMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "team_id") public Long teamId;
    @Column(name = "user_id") public Long userId;
    @Column(name = "member_role") public String memberRole = "MEMBER";
    public String title;
    @Column(name = "joined_at") public Instant joinedAt = Instant.now();
    public TeamMember() {}
}
