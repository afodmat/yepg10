package com.yep.platform.notification;

import com.yep.platform.common.PageResponse;
import com.yep.platform.notification.NotificationService.*;
import com.yep.platform.security.AuthUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping public PageResponse<NotificationView> list(@AuthenticationPrincipal AuthUser me, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.list(me.id(), page, size); }
    @GetMapping("/unread-count") public Map<String, Long> unread(@AuthenticationPrincipal AuthUser me) { return Map.of("count", service.unread(me.id())); }
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { service.markRead(me.id(), id); }
    @PostMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) public void readAll(@AuthenticationPrincipal AuthUser me) { service.markAllRead(me.id()); }
    @GetMapping("/preferences") public List<PreferenceView> prefs(@AuthenticationPrincipal AuthUser me) { return service.preferences(me.id()); }
    @PutMapping("/preferences") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPref(@AuthenticationPrincipal AuthUser me, @RequestBody PreferenceView body) { service.setPreference(me.id(), body.type(), body.enabled()); }
}
