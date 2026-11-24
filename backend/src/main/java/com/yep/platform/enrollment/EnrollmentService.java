package com.yep.platform.enrollment;

import com.yep.platform.activity.*;
import com.yep.platform.audit.AuditService;
import com.yep.platform.certificate.RecognitionService;
import com.yep.platform.common.*;
import com.yep.platform.notification.*;
import com.yep.platform.user.UserDirectory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    public record EnrollmentView(Long id, Long activityId, String title, String category, String level, EnrollmentStatus status, int progressPct,
                                 int completedLessons, int lessonsTotal, BigDecimal finalScore, Instant enrolledAt, Instant completedAt,
                                 Long assessmentId, boolean assessmentPassed) {}
    public record RosterRow(Long userId, String name, EnrollmentStatus status, int progressPct, BigDecimal finalScore, Instant enrolledAt) {}

    private final EnrollmentRepository repo;
    private final LearningActivityRepository activities;
    private final NotificationService notifications;
    private final RecognitionService recognition;
    private final NamedParameterJdbcTemplate jdbc;
    private final UserDirectory directory;

    public EnrollmentService(EnrollmentRepository repo, LearningActivityRepository activities, NotificationService notifications,
                             RecognitionService recognition, NamedParameterJdbcTemplate jdbc, UserDirectory directory) {
        this.repo = repo; this.activities = activities; this.notifications = notifications; this.recognition = recognition; this.jdbc = jdbc; this.directory = directory;
    }

    @Transactional
    public EnrollmentView enroll(Long userId, Long activityId) {
        var a = activities.findById(activityId).orElseThrow(() -> ApiException.notFound("Learning activity not found"));
        if (a.status != ActivityStatus.OPEN) throw ApiException.conflict("This activity is not open for enrollment.");
        var existing = repo.findByUserIdAndActivityId(userId, activityId);
        if (existing.isPresent() && existing.get().status != EnrollmentStatus.CANCELLED) throw ApiException.conflict("You are already enrolled in this activity.");
        if (repo.countActive(activityId) >= a.capacity) throw ApiException.conflict("This activity is full.");
        var e = existing.orElseGet(Enrollment::new);
        e.userId = userId; e.activityId = activityId; e.status = EnrollmentStatus.ENROLLED; e.enrolledAt = Instant.now();
        e.completedLessons = 0; e.progressPct = 0; e.completedAt = null; e.finalScore = null;
        repo.save(e);
        notifications.notify(userId, NotificationType.ENROLLMENT, "You're enrolled", "Welcome to " + a.title + ". Start your first lesson whenever you're ready.", "mylearning");
        return views(userId, List.of(e)).get(0);
    }

    @Transactional
    public void cancel(Long userId, Long activityId) {
        var e = repo.findByUserIdAndActivityId(userId, activityId).orElseThrow(() -> ApiException.notFound("You are not enrolled in this activity."));
        if (e.status == EnrollmentStatus.COMPLETED) throw ApiException.conflict("Completed activities cannot be cancelled.");
        e.status = EnrollmentStatus.CANCELLED;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentView> mine(Long userId) { return views(userId, repo.findByUserIdAndStatusNotOrderByEnrolledAtDesc(userId, EnrollmentStatus.CANCELLED)); }

    @Transactional
    public EnrollmentView completeLesson(Long userId, Long activityId) {
        var e = repo.findByUserIdAndActivityId(userId, activityId).filter(x -> x.status == EnrollmentStatus.ENROLLED)
                .orElseThrow(() -> ApiException.notFound("You are not currently enrolled in this activity."));
        var a = activities.findById(activityId).orElseThrow();
        if (e.completedLessons >= a.lessonsTotal) throw ApiException.conflict("All lessons are already complete.");
        e.completedLessons++;
        refresh(e, a);
        return views(userId, List.of(e)).get(0);
    }

    /** Called by the assessment module after each graded attempt. */
    @Transactional
    public void onAssessmentResult(Long userId, Long activityId, BigDecimal scorePct) {
        repo.findByUserIdAndActivityId(userId, activityId).filter(x -> x.status != EnrollmentStatus.CANCELLED).ifPresent(e -> {
            if (e.finalScore == null || scorePct.compareTo(e.finalScore) > 0) e.finalScore = scorePct;
            refresh(e, activities.findById(activityId).orElseThrow());
        });
    }

    public boolean isEnrolled(Long userId, Long activityId) {
        return repo.findByUserIdAndActivityId(userId, activityId).filter(x -> x.status == EnrollmentStatus.ENROLLED || x.status == EnrollmentStatus.COMPLETED).isPresent();
    }

    @Transactional(readOnly = true)
    public PageResponse<RosterRow> roster(Long activityId, int page, int size) {
        var p = repo.findByActivityIdAndStatusNot(activityId, EnrollmentStatus.CANCELLED, PageResponse.request(page, size, Sort.by("enrolledAt")));
        var names = directory.names(p.getContent().stream().map(e -> e.userId).toList());
        return PageResponse.of(p, e -> new RosterRow(e.userId, names.get(e.userId), e.status, e.progressPct, e.finalScore, e.enrolledAt));
    }

    // progress = lessons (90% if the activity has an assessment, else 100%) + 10% for passing the assessment
    private void refresh(Enrollment e, LearningActivity a) {
        Long assessmentId = assessmentIds(List.of(a.id)).get(a.id);
        boolean passed = assessmentId == null || passedAssessments(e.userId, List.of(a.id)).contains(a.id);
        int lessonPart = assessmentId == null ? 100 : 90;
        int pct = e.completedLessons * lessonPart / a.lessonsTotal + (assessmentId != null && passed ? 10 : 0);
        e.progressPct = (short) Math.min(100, pct);
        if (e.status == EnrollmentStatus.ENROLLED && e.completedLessons >= a.lessonsTotal && passed) {
            e.status = EnrollmentStatus.COMPLETED; e.progressPct = 100; e.completedAt = Instant.now();
            repo.flush();
            recognition.onActivityCompleted(e.userId, a.id);
        }
    }

    private List<EnrollmentView> views(Long userId, List<Enrollment> list) {
        if (list.isEmpty()) return List.of();
        var ids = list.stream().map(e -> e.activityId).toList();
        Map<Long, LearningActivity> acts = new HashMap<>();
        activities.findAllById(ids).forEach(a -> acts.put(a.id, a));
        var assessment = assessmentIds(ids);
        var passed = passedAssessments(userId, ids);
        return list.stream().map(e -> {
            var a = acts.get(e.activityId);
            return new EnrollmentView(e.id, e.activityId, a.title, a.category, a.level, e.status, e.progressPct, e.completedLessons, a.lessonsTotal,
                    e.finalScore, e.enrolledAt, e.completedAt, assessment.get(a.id), passed.contains(a.id));
        }).toList();
    }

    private Map<Long, Long> assessmentIds(Collection<Long> activityIds) {
        Map<Long, Long> out = new HashMap<>();
        jdbc.query("select activity_id, id from assessments where activity_id in (:ids)", new MapSqlParameterSource("ids", activityIds),
                rs -> { out.put(rs.getLong(1), rs.getLong(2)); });
        return out;
    }
    private Set<Long> passedAssessments(Long userId, Collection<Long> activityIds) {
        Set<Long> out = new HashSet<>();
        jdbc.query("select distinct a.activity_id from assessments a join assessment_attempts t on t.assessment_id = a.id where t.user_id = :uid and t.passed and a.activity_id in (:ids)",
                new MapSqlParameterSource("uid", userId).addValue("ids", activityIds), rs -> { out.add(rs.getLong(1)); });
        return out;
    }
}
