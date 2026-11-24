package com.yep.platform.achievement;

import com.yep.platform.achievement.AchievementRepositories.*;
import com.yep.platform.notification.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {
    public record AchievementView(Long id, String name, String description, String icon, String criteria, boolean earned, Instant awardedAt) {}
    private record Rule(String code, String sql, int threshold) {}

    /** Each rule counts activity for a user (the SQL's ? placeholders are all the user id). */
    private static final List<Rule> RULES = List.of(
        new Rule("DIGITAL_PIONEER", "select count(*) from enrollments where user_id = ? and status = 'COMPLETED'", 1),
        new Rule("LEARNING_CHAMPION", "select count(*) from enrollments where user_id = ? and status = 'COMPLETED'", 3),
        new Rule("VIRTUAL_EXCHANGE_PARTICIPANT", "select count(*) from exchange_participants where user_id = ?", 1),
        new Rule("TEAM_BUILDER", "select count(*) from team_members where user_id = ?", 1),
        new Rule("THOUGHTFUL_CONTRIBUTOR", "select (select count(*) from project_updates where author_id = ?) + (select count(*) from project_comments where author_id = ?)", 3),
        new Rule("PROJECT_INNOVATOR", "select count(*) from impact_projects where leader_id = ?", 1));

    private final AchievementRepository achievements;
    private final UserAchievementRepository earned;
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;

    public AchievementService(AchievementRepository achievements, UserAchievementRepository earned, JdbcTemplate jdbc, NotificationService notifications) {
        this.achievements = achievements; this.earned = earned; this.jdbc = jdbc; this.notifications = notifications;
    }

    /** Re-checks every rule for the user and awards anything newly earned. Safe to call after any relevant event. */
    @Transactional
    public void evaluate(Long userId) {
        for (var rule : RULES) {
            var ach = achievements.findByCode(rule.code()).filter(a -> a.active);
            if (ach.isEmpty() || earned.existsByUserIdAndAchievementId(userId, ach.get().id)) continue;
            int params = rule.sql().length() - rule.sql().replace("?", "").length();
            Object[] args = new Object[params];
            Arrays.fill(args, userId);
            Long count = jdbc.queryForObject(rule.sql(), Long.class, args);
            if (count != null && count >= rule.threshold()) award(userId, ach.get());
        }
    }

    @Transactional
    public void award(Long userId, Achievement a) {
        if (earned.existsByUserIdAndAchievementId(userId, a.id)) return;
        var ua = new UserAchievement();
        ua.userId = userId; ua.achievementId = a.id;
        earned.save(ua);
        notifications.notify(userId, NotificationType.ACHIEVEMENT, "Achievement unlocked: " + a.name, a.description, "achievements");
    }

    @Transactional(readOnly = true)
    public List<AchievementView> forUser(Long userId) {
        Map<Long, Instant> mine = new HashMap<>();
        earned.findByUserId(userId).forEach(u -> mine.put(u.achievementId, u.awardedAt));
        return achievements.findByActiveTrueOrderById().stream()
                .map(a -> new AchievementView(a.id, a.name, a.description, a.icon, a.criteria, mine.containsKey(a.id), mine.get(a.id))).toList();
    }
}
