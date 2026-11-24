package com.yep.platform.dashboard;

import com.yep.platform.dashboard.DashboardService.DashboardView;
import com.yep.platform.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping public DashboardView get(@AuthenticationPrincipal AuthUser me) { return service.build(me); }
}
