package com.yep.platform.project;

import com.yep.platform.common.PageResponse;
import com.yep.platform.project.ProjectDtos.*;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }

    /** Public showcase list (summaries only). `mine=true` needs a login. */
    @GetMapping public PageResponse<ProjectView> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String category, @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size, @AuthenticationPrincipal AuthUser me) { return service.list(q, category, mine, page, size, me); }
    @GetMapping("/{id}") public ProjectView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.get(id, me); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProjectView create(@Valid @RequestBody ProjectRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.create(r, me, req.getRemoteAddr()); }
    @PutMapping("/{id}") public ProjectView update(@PathVariable Long id, @Valid @RequestBody ProjectRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.update(id, r, me, req.getRemoteAddr()); }
    @PatchMapping("/{id}/status") public ProjectView status(@PathVariable Long id, @Valid @RequestBody StatusRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.setStatus(id, r.status(), me, req.getRemoteAddr()); }

    @PostMapping("/{id}/updates") public ProjectView update(@PathVariable Long id, @Valid @RequestBody UpdateRequest r, @AuthenticationPrincipal AuthUser me) { return service.postUpdate(id, r, me); }
    @PatchMapping("/{id}/milestones/{mid}") public ProjectView milestone(@PathVariable Long id, @PathVariable Long mid, @Valid @RequestBody MilestoneRequest r, @AuthenticationPrincipal AuthUser me) { return service.patchMilestone(id, mid, r.status(), me); }
    @PostMapping("/{id}/comments") public ProjectView comment(@PathVariable Long id, @Valid @RequestBody CommentRequest r, @AuthenticationPrincipal AuthUser me) { return service.comment(id, r.content(), me); }
    @PostMapping("/{id}/attachments") public ProjectView attach(@PathVariable Long id, @Valid @RequestBody AttachRequest r, @AuthenticationPrincipal AuthUser me) { return service.attach(id, r.fileId(), me); }
}
