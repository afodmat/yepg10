package com.yep.platform.admin;

import com.yep.platform.achievement.Achievement;
import com.yep.platform.admin.AdminDtos.*;
import com.yep.platform.certificate.CertificateService;
import com.yep.platform.certificate.CertificateService.CertificateView;
import com.yep.platform.common.PageResponse;
import com.yep.platform.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Everything under /api/admin requires ADMIN (URL rule in SecurityConfig plus @PreAuthorize here as defence in depth). */
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService service;
    private final CertificateService certificates;
    public AdminController(AdminService service, CertificateService certificates) { this.service = service; this.certificates = certificates; }

    @GetMapping("/stats") public Map<String, Long> stats() { return service.stats(); }

    @GetMapping("/users")
    public PageResponse<UserRow> users(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String role, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "created") String sort, @RequestParam(defaultValue = "desc") String dir, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.users(q, role, status, sort, dir, page, size);
    }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    public UserRow create(@Valid @RequestBody CreateUserRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.createUser(r, me, req.getRemoteAddr()); }
    @PatchMapping("/users/{id}/role") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void role(@PathVariable Long id, @Valid @RequestBody RoleRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.setRole(id, r.role(), me, req.getRemoteAddr()); }
    @PatchMapping("/users/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(@PathVariable Long id, @Valid @RequestBody StatusRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.setStatus(id, r.status(), me, req.getRemoteAddr()); }

    @GetMapping("/audit-logs")
    public PageResponse<AuditRow> audit(@RequestParam(defaultValue = "") String action, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.auditLogs(action, page, size); }

    @GetMapping("/settings") public Map<String, String> settings() { return service.settings(); }
    @PutMapping("/settings") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void settings(@RequestBody Map<String, String> body, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.updateSettings(body, me, req.getRemoteAddr()); }

    @PostMapping("/notifications")
    public Map<String, Integer> broadcast(@Valid @RequestBody BroadcastRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return Map.of("recipients", service.broadcast(r, me, req.getRemoteAddr())); }

    @GetMapping("/achievements") public List<Achievement> achievements() { return service.allAchievements(); }
    @PostMapping("/achievements") @ResponseStatus(HttpStatus.CREATED)
    public Achievement createAchievement(@Valid @RequestBody AchievementRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.saveAchievement(null, r, me, req.getRemoteAddr()); }
    @PutMapping("/achievements/{id}")
    public Achievement updateAchievement(@PathVariable Long id, @Valid @RequestBody AchievementRequest r, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { return service.saveAchievement(id, r, me, req.getRemoteAddr()); }
    @PostMapping("/users/{userId}/achievements/{achievementId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void award(@PathVariable Long userId, @PathVariable Long achievementId, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { service.awardManually(userId, achievementId, me, req.getRemoteAddr()); }

    @GetMapping("/certificates") public PageResponse<CertificateView> certificates(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return certificates.all(page, size); }
    @PostMapping("/certificates/{id}/revoke") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, @AuthenticationPrincipal AuthUser me, HttpServletRequest req) { certificates.revoke(id, me, req.getRemoteAddr()); }
}
