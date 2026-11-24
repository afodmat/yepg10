package com.yep.platform.assessment;

import com.yep.platform.assessment.AssessmentDtos.*;
import com.yep.platform.common.PageResponse;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AssessmentController {
    private final AssessmentService service;
    public AssessmentController(AssessmentService service) { this.service = service; }

    @PutMapping("/api/activities/{activityId}/assessment") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public AssessmentView save(@PathVariable Long activityId, @Valid @RequestBody AssessmentRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.save(activityId, r, me, req.getRemoteAddr()); }

    @GetMapping("/api/activities/{activityId}/assessment")
    public AssessmentView get(@PathVariable Long activityId, @AuthenticationPrincipal AuthUser me) { return service.forActivity(activityId, me); }

    @PostMapping("/api/assessments/{id}/submit")
    public AttemptResult submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest r, @AuthenticationPrincipal AuthUser me) { return service.submit(id, r, me); }

    @GetMapping("/api/assessments/{id}/attempts/mine")
    public List<AttemptRow> mine(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.myAttempts(id, me); }

    @GetMapping("/api/assessments/{id}/results") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public PageResponse<AttemptRow> results(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal AuthUser me) { return service.results(id, page, size, me); }
}
