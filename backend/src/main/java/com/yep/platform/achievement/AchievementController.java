package com.yep.platform.achievement;

import com.yep.platform.achievement.AchievementService.AchievementView;
import com.yep.platform.security.AuthUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/achievements")
public class AchievementController {
    private final AchievementService service;
    public AchievementController(AchievementService service) { this.service = service; }
    @GetMapping public List<AchievementView> mine(@AuthenticationPrincipal AuthUser me) { return service.forUser(me.id()); }
}
