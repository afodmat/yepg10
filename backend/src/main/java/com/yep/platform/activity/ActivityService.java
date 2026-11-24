package com.yep.platform.activity;

import com.yep.platform.activity.ActivityDtos.*;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.enrollment.EnrollmentRepository;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.Role;
import com.yep.platform.user.UserDirectory;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final LearningActivityRepository repo;
    private final EnrollmentRepository enrollments;
    private final UserDirectory directory;
    private final AuditService audit;

    public ActivityService(LearningActivityRepository repo, EnrollmentRepository enrollments, UserDirectory directory, AuditService audit) {
        this.repo = repo; this.enrollments = enrollments; this.directory = directory; this.audit = audit;
    }

    static boolean isStaff(AuthUser u) { return u != null && (u.role() == Role.FACILITATOR || u.role() == Role.ADMIN); }

    @Transactional(readOnly = true)
    public PageResponse<ActivityView> search(String q, String category, String level, int page, int size, AuthUser me) {
        var result = repo.search(nz(q), nz(category), nz(level), isStaff(me),
                PageResponse.request(page, size, Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"))));
        Map<Long, Long> counts = counts(result.getContent().stream().map(a -> a.id).toList());
        Map<Long, String> names = directory.names(result.getContent().stream().map(a -> a.facilitatorId).toList());
        return PageResponse.of(result, a -> view(a, counts.getOrDefault(a.id, 0L), names.get(a.facilitatorId)));
    }

    @Transactional(readOnly = true)
    public ActivityView get(Long id, AuthUser me) {
        var a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Learning activity not found"));
        if (a.status == ActivityStatus.DRAFT && !isStaff(me)) throw ApiException.notFound("Learning activity not found");
        return view(a, enrollments.countActive(id), directory.name(a.facilitatorId));
    }

    @Transactional
    public ActivityView create(ActivityRequest r, AuthUser me, String ip) {
        var a = new LearningActivity();
        apply(a, r);
        a.facilitatorId = me.id();
        repo.save(a);
        audit.record(null, me.email(), "ACTIVITY_CREATED", "LearningActivity", String.valueOf(a.id), Map.of("title", a.title), ip);
        return view(a, 0, directory.name(a.facilitatorId));
    }

    @Transactional
    public ActivityView update(Long id, ActivityRequest r, AuthUser me, String ip) {
        var a = owned(id, me);
        apply(a, r);
        audit.record(null, me.email(), "ACTIVITY_UPDATED", "LearningActivity", String.valueOf(id), Map.of("status", a.status.name()), ip);
        return view(a, enrollments.countActive(id), directory.name(a.facilitatorId));
    }

    @Transactional
    public void delete(Long id, AuthUser me, String ip) {
        var a = owned(id, me);
        if (enrollments.countActive(id) > 0) throw ApiException.conflict("This activity has enrolled participants. Close it instead of deleting it.");
        repo.delete(a);
        audit.record(null, me.email(), "ACTIVITY_DELETED", "LearningActivity", String.valueOf(id), Map.of("title", a.title), ip);
    }

    /** Facilitators manage their own activities; admins manage all. */
    public LearningActivity owned(Long id, AuthUser me) {
        var a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Learning activity not found"));
        if (me.role() != Role.ADMIN && !Objects.equals(a.facilitatorId, me.id())) throw ApiException.forbidden("You can only manage activities you facilitate.");
        return a;
    }

    private void apply(LearningActivity a, ActivityRequest r) {
        if (r.startDate() != null && r.endDate() != null && r.endDate().isBefore(r.startDate())) throw ApiException.badRequest("End date must be on or after the start date.");
        a.title = r.title().trim(); a.description = r.description().trim(); a.category = r.category().trim(); a.level = r.level().trim();
        a.objectives = r.objectives(); a.startDate = r.startDate(); a.endDate = r.endDate(); a.durationHours = r.durationHours();
        a.capacity = r.capacity(); a.lessonsTotal = r.lessonsTotal(); a.status = r.status(); a.thumbnailUrl = r.thumbnailUrl();
    }

    private Map<Long, Long> counts(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return enrollments.countsFor(ids).stream().collect(Collectors.toMap(o -> (Long) o[0], o -> (Long) o[1]));
    }
    private static ActivityView view(LearningActivity a, long enrolled, String facilitator) {
        return new ActivityView(a.id, a.title, a.description, a.category, a.level, a.objectives, a.facilitatorId, facilitator, a.startDate, a.endDate,
                a.durationHours, a.capacity, enrolled, a.lessonsTotal, a.status, a.thumbnailUrl);
    }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
}
