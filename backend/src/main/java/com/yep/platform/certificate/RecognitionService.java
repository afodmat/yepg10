package com.yep.platform.certificate;

import com.yep.platform.achievement.AchievementService;
import com.yep.platform.activity.LearningActivityRepository;
import com.yep.platform.audit.AuditService;
import com.yep.platform.notification.*;
import java.security.SecureRandom;
import java.time.Year;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reacts to learning milestones: issues the certificate and evaluates achievements. */
@Service
public class RecognitionService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no look-alike characters
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificates;
    private final LearningActivityRepository activities;
    private final AchievementService achievements;
    private final NotificationService notifications;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public RecognitionService(CertificateRepository certificates, LearningActivityRepository activities, AchievementService achievements,
                              NotificationService notifications, AuditService audit, JdbcTemplate jdbc) {
        this.certificates = certificates; this.activities = activities; this.achievements = achievements; this.notifications = notifications; this.audit = audit; this.jdbc = jdbc;
    }

    @Transactional
    public void onActivityCompleted(Long userId, Long activityId) {
        if (certificates.findByUserIdAndActivityId(userId, activityId).isEmpty()) {
            var c = new Certificate();
            c.userId = userId; c.activityId = activityId;
            long seq = jdbc.queryForObject("select nextval('certificate_seq')", Long.class);
            c.certificateNumber = "YEP-" + Year.now().getValue() + "-" + String.format("%05d", seq);
            c.verificationCode = randomCode(12);
            certificates.save(c);
            var title = activities.findById(activityId).map(a -> a.title).orElse("a learning activity");
            notifications.notify(userId, NotificationType.CERTIFICATE, "Your certificate is ready", "You earned a certificate for " + title + ".", "achievements");
            audit.record(null, "system", "CERTIFICATE_ISSUED", "Certificate", String.valueOf(c.id), Map.of("userId", userId, "activityId", activityId), null);
        }
        achievements.evaluate(userId);
    }

    private static String randomCode(int n) {
        var sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
