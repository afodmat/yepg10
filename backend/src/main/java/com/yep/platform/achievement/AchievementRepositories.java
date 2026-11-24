package com.yep.platform.achievement;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public final class AchievementRepositories {
    private AchievementRepositories() {
    }

    public interface AchievementRepository extends JpaRepository<Achievement, Long> {
        List<Achievement> findByActiveTrueOrderById();

        Optional<Achievement> findByCode(String code);
    }

    public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
        List<UserAchievement> findByUserId(Long userId);

        boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);
    }
}
