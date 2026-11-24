package com.yep.platform.team;

import com.yep.platform.common.PageResponse;
import com.yep.platform.security.AuthUser;
import com.yep.platform.team.TeamDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service) { this.service = service; }

    @GetMapping("/api/teams")
    public PageResponse<TeamView> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "false") boolean mine, @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "12") int size, @AuthenticationPrincipal AuthUser me) { return service.list(q.trim(), mine, page, size, me); }
    @GetMapping("/api/teams/{id}") public TeamView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.get(id, me); }
    @PostMapping("/api/teams") @ResponseStatus(HttpStatus.CREATED)
    public TeamView create(@Valid @RequestBody TeamRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.create(r, me, req.getRemoteAddr()); }
    @PutMapping("/api/teams/{id}")
    public TeamView update(@PathVariable Long id, @Valid @RequestBody TeamRequest r, @RequestParam(required = false) String status, @AuthenticationPrincipal AuthUser me) { return service.update(id, r, status, me); }

    @PostMapping("/api/teams/{id}/join") public TeamView join(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.join(id, me); }
    @DeleteMapping("/api/teams/{id}/members/me") @ResponseStatus(HttpStatus.NO_CONTENT) public void leave(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.leave(id, me); }
    @DeleteMapping("/api/teams/{id}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @PathVariable Long userId, @AuthenticationPrincipal AuthUser me) { service.removeMember(id, userId, me); }
    @PutMapping("/api/teams/{id}/members/{userId}/title") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void title(@PathVariable Long id, @PathVariable Long userId, @Valid @RequestBody TitleRequest r, @AuthenticationPrincipal AuthUser me) { service.setTitle(id, userId, r.title(), me); }

    @PostMapping("/api/teams/{id}/invitations") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> invite(@PathVariable Long id, @Valid @RequestBody InviteRequest r, @AuthenticationPrincipal AuthUser me) { service.invite(id, r.email(), me); return Map.of("message", "Invitation sent."); }
    @GetMapping("/api/invitations/mine") public List<InvitationView> invitations(@AuthenticationPrincipal AuthUser me) { return service.myInvitations(me); }
    @PostMapping("/api/invitations/{id}/accept") public TeamView accept(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.respond(id, true, me); }
    @PostMapping("/api/invitations/{id}/decline") @ResponseStatus(HttpStatus.NO_CONTENT) public void decline(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.respond(id, false, me); }
}
