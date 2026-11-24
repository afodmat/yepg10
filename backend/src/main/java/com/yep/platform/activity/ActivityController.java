package com.yep.platform.activity;

import com.yep.platform.activity.ActivityDtos.*;
import com.yep.platform.common.PageResponse;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service = service; }

    /** Public: visitors can browse. Drafts are only returned to staff. */
    @GetMapping
    public PageResponse<ActivityView> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String category, @RequestParam(defaultValue = "") String level,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size, @AuthenticationPrincipal AuthUser me) {
        return service.search(q, category, level, page, size, me);
    }
    @GetMapping("/{id}") public ActivityView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.get(id, me); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityView create(@Valid @RequestBody ActivityRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.create(r, me, req.getRemoteAddr()); }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityView update(@PathVariable Long id, @Valid @RequestBody ActivityRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.update(id, r, me, req.getRemoteAddr()); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.delete(id, me, req.getRemoteAddr()); }
}
