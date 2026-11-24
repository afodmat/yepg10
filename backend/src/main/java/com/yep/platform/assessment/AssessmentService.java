package com.yep.platform.assessment;

import com.yep.platform.activity.ActivityService;
import com.yep.platform.assessment.AssessmentDtos.*;
import com.yep.platform.assessment.AssessmentRepositories.*;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.enrollment.EnrollmentService;
import com.yep.platform.notification.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.Role;
import com.yep.platform.user.UserDirectory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final OptionRepository options;
    private final AttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final ActivityService activities;
    private final EnrollmentService enrollments;
    private final NotificationService notifications;
    private final AuditService audit;
    private final UserDirectory directory;

    public AssessmentService(AssessmentRepository assessments, QuestionRepository questions, OptionRepository options, AttemptRepository attempts,
                             AttemptAnswerRepository answers, ActivityService activities, EnrollmentService enrollments, NotificationService notifications,
                             AuditService audit, UserDirectory directory) {
        this.assessments = assessments; this.questions = questions; this.options = options; this.attempts = attempts; this.answers = answers;
        this.activities = activities; this.enrollments = enrollments; this.notifications = notifications; this.audit = audit; this.directory = directory;
    }

    @Transactional
    public AssessmentView save(Long activityId, AssessmentRequest r, AuthUser me, String ip) {
        activities.owned(activityId, me);
        for (var q : r.questions()) {
            long correct = q.options().stream().filter(OptionInput::correct).count();
            if (correct != 1) throw ApiException.badRequest("Each question needs exactly one correct option: \"" + abbreviate(q.text()) + "\"");
        }
        var existing = assessments.findByActivityId(activityId);
        if (existing.isPresent() && attempts.countByAssessmentId(existing.get().id) > 0)
            throw ApiException.conflict("Participants have already attempted this assessment, so it can no longer be replaced.");
        existing.ifPresent(a -> { assessments.delete(a); assessments.flush(); });
        var a = new Assessment();
        a.activityId = activityId; a.title = r.title().trim(); a.passMark = r.passMark().shortValue(); a.maxAttempts = r.maxAttempts().shortValue();
        assessments.save(a);
        short qPos = 0;
        for (var qi : r.questions()) {
            var q = new Question();
            q.assessmentId = a.id; q.text = qi.text().trim(); q.points = (short) (qi.points() == null ? 1 : qi.points()); q.position = qPos++;
            questions.save(q);
            short oPos = 0;
            for (var oi : qi.options()) {
                var o = new QuestionOption();
                o.questionId = q.id; o.text = oi.text().trim(); o.correct = oi.correct(); o.position = oPos++;
                options.save(o);
            }
        }
        audit.record(null, me.email(), "ASSESSMENT_SAVED", "Assessment", String.valueOf(a.id), Map.of("activityId", activityId, "questions", r.questions().size()), ip);
        return view(a, me);
    }

    @Transactional(readOnly = true)
    public AssessmentView forActivity(Long activityId, AuthUser me) {
        var a = assessments.findByActivityId(activityId).orElseThrow(() -> ApiException.notFound("This activity has no assessment."));
        boolean staff = me.role() == Role.ADMIN || me.role() == Role.FACILITATOR;
        if (!staff && !enrollments.isEnrolled(me.id(), activityId)) throw ApiException.forbidden("Enroll in this activity to take its assessment.");
        return view(a, me);
    }

    @Transactional
    public AttemptResult submit(Long assessmentId, SubmitRequest req, AuthUser me) {
        var a = assessments.findById(assessmentId).orElseThrow(() -> ApiException.notFound("Assessment not found"));
        if (!enrollments.isEnrolled(me.id(), a.activityId)) throw ApiException.forbidden("Enroll in this activity to take its assessment.");
        long used = attempts.countByAssessmentIdAndUserId(a.id, me.id());
        if (used >= a.maxAttempts) throw ApiException.conflict("You have used all " + a.maxAttempts + " attempts.");

        var qs = questions.findByAssessmentIdOrderByPosition(a.id);
        Map<Long, List<QuestionOption>> opts = options.findByQuestionIdInOrderByPosition(qs.stream().map(q -> q.id).toList())
                .stream().collect(Collectors.groupingBy(o -> o.questionId));
        int earned = 0, total = 0, correctCount = 0;
        List<AttemptAnswer> rows = new ArrayList<>();
        for (var q : qs) {
            total += q.points;
            Long chosen = req.answers().get(q.id);
            var option = opts.getOrDefault(q.id, List.of()).stream().filter(o -> o.id.equals(chosen)).findFirst(); // ignores option ids from other questions
            boolean ok = option.map(o -> o.correct).orElse(false);
            if (ok) { earned += q.points; correctCount++; }
            var row = new AttemptAnswer();
            row.questionId = q.id; row.optionId = option.map(o -> o.id).orElse(null); row.correct = ok;
            rows.add(row);
        }
        var score = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(earned * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        boolean passed = score.compareTo(BigDecimal.valueOf(a.passMark)) >= 0;
        var attempt = new AssessmentAttempt();
        attempt.assessmentId = a.id; attempt.userId = me.id(); attempt.scorePct = score; attempt.passed = passed;
        attempts.save(attempt);
        rows.forEach(r -> r.attemptId = attempt.id);
        answers.saveAll(rows);
        attempts.flush();

        enrollments.onAssessmentResult(me.id(), a.activityId, score);
        notifications.notify(me.id(), NotificationType.ASSESSMENT_RESULT, passed ? "Assessment passed" : "Assessment result",
                a.title + ": you scored " + score.stripTrailingZeros().toPlainString() + "% (pass mark " + a.passMark + "%).", "mylearning");
        int left = (int) (a.maxAttempts - used - 1);
        String feedback = passed ? "Well done! You've passed this assessment."
                : left > 0 ? "Not quite there yet. Review the lessons and try again — you have " + left + " attempt(s) left."
                : "You have used all attempts. Please speak to your facilitator about next steps.";
        return new AttemptResult(attempt.id, score, passed, correctCount, qs.size(), left, feedback);
    }

    @Transactional(readOnly = true)
    public List<AttemptRow> myAttempts(Long assessmentId, AuthUser me) {
        return attempts.findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(assessmentId, me.id()).stream()
                .map(t -> new AttemptRow(t.id, t.userId, null, t.scorePct, t.passed, t.submittedAt)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptRow> results(Long assessmentId, int page, int size, AuthUser me) {
        var a = assessments.findById(assessmentId).orElseThrow(() -> ApiException.notFound("Assessment not found"));
        activities.owned(a.activityId, me);
        var p = attempts.findByAssessmentId(assessmentId, PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "submittedAt")));
        var names = directory.names(p.getContent().stream().map(t -> t.userId).toList());
        return PageResponse.of(p, t -> new AttemptRow(t.id, t.userId, names.get(t.userId), t.scorePct, t.passed, t.submittedAt));
    }

    private AssessmentView view(Assessment a, AuthUser me) {
        boolean staff = me.role() == Role.ADMIN || me.role() == Role.FACILITATOR;
        var qs = questions.findByAssessmentIdOrderByPosition(a.id);
        Map<Long, List<QuestionOption>> opts = options.findByQuestionIdInOrderByPosition(qs.stream().map(q -> q.id).toList())
                .stream().collect(Collectors.groupingBy(o -> o.questionId));
        var qv = qs.stream().map(q -> new QuestionView(q.id, q.text, q.points,
                opts.getOrDefault(q.id, List.of()).stream().map(o -> new OptionView(o.id, o.text, staff ? o.correct : null)).toList())).toList();
        var mine = attempts.findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(a.id, me.id());
        return new AssessmentView(a.id, a.activityId, a.title, a.passMark, a.maxAttempts, mine.size(), mine.stream().anyMatch(t -> t.passed), qv);
    }

    private static String abbreviate(String s) { return s.length() > 40 ? s.substring(0, 40) + "…" : s; }
}
