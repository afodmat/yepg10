package com.yep.platform.enrollment;

import com.yep.platform.activity.ActivityService;
import com.yep.platform.common.PageResponse;
import com.yep.platform.enrollment.EnrollmentService.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.Role;
import com.yep.platform.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class EnrollmentController {
    private final EnrollmentService service;
    private final ActivityService activities;
    public EnrollmentController(EnrollmentService service, ActivityService activities) { this.service = service; this.activities = activities; }

    @PostMapping("/api/activities/{id}/enroll") @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentView enroll(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.enroll(me.id(), id); }

    @DeleteMapping("/api/activities/{id}/enroll") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.cancel(me.id(), id); }

    @GetMapping("/api/enrollments/mine") public List<EnrollmentView> mine(@AuthenticationPrincipal AuthUser me) { return service.mine(me.id()); }

    @PostMapping("/api/activities/{id}/lessons/complete")
    public EnrollmentView lesson(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.completeLesson(me.id(), id); }

    /** Facilitators monitor participants of their own activities; admins see all. */
    @GetMapping("/api/activities/{id}/enrollments") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public PageResponse<RosterRow> roster(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal AuthUser me) {
        activities.owned(id, me);
        return service.roster(id, page, size);
    }
}
