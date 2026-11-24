package com.yep.platform.mentorship;

import com.yep.platform.common.PageResponse;
import com.yep.platform.mentorship.MentorshipDtos.*;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class MentorshipController {
    private final MentorshipService service;
    public MentorshipController(MentorshipService service) { this.service = service; }

    @GetMapping("/api/mentors") public PageResponse<MentorView> mentors(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) { return service.listMentors(q, page, size); }
    @GetMapping("/api/mentors/me") @PreAuthorize("hasRole('MENTOR')")
    public MentorProfileRequest myProfile(@AuthenticationPrincipal AuthUser me) { return service.myMentorProfile(me.id()).orElse(null); }
    @PutMapping("/api/mentors/me") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('MENTOR')")
    public void saveProfile(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody MentorProfileRequest r) { service.saveMentorProfile(me.id(), r); }

    @PostMapping("/api/mentorship/requests") @ResponseStatus(HttpStatus.CREATED) public void request(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody MentorRequestInput r) { service.request(me, r); }
    @GetMapping("/api/mentorship/requests/sent") public List<RequestView> sent(@AuthenticationPrincipal AuthUser me) { return service.sent(me); }
    @GetMapping("/api/mentorship/requests/incoming") @PreAuthorize("hasRole('MENTOR')") public List<RequestView> incoming(@AuthenticationPrincipal AuthUser me) { return service.incoming(me); }
    @PostMapping("/api/mentorship/requests/{id}/respond") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('MENTOR')")
    public void respond(@PathVariable Long id, @RequestBody RespondBody b, @AuthenticationPrincipal AuthUser me) { service.respond(me, id, b.accept()); }

    @GetMapping("/api/mentorship/mine") public List<AssignmentView> mine(@AuthenticationPrincipal AuthUser me) { return service.mine(me); }
    @GetMapping("/api/mentorship/assignments") @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public PageResponse<AssignmentView> all(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.all(page, size); }
    @PostMapping("/api/mentorship/assignments") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public void assign(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody AssignBody b, HttpServletRequest req) { service.assign(me, b, req.getRemoteAddr()); }
    @DeleteMapping("/api/mentorship/assignments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public void end(@PathVariable Long id, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.end(me, id, req.getRemoteAddr()); }
}
