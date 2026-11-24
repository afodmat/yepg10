package com.yep.platform.achievement;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id")
    public Long userId;
    @Column(name = "achievement_id")
    public Long achievementId;
    @Column(name = "awarded_at")
    public Instant awardedAt = Instant.now();

    public UserAchievement() {
    }
}
