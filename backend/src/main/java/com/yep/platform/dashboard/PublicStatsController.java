package com.yep.platform.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Aggregate, non-personal counts for the public home page (real numbers, no per-user data). */
@RestController @RequestMapping("/api/public")
public class PublicStatsController {
    private final JdbcTemplate jdbc;
    public PublicStatsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("participants", jdbc.queryForObject("select count(*) from users where role = 'PARTICIPANT' and status = 'ACTIVE'", Long.class));
        m.put("countries", jdbc.queryForObject("select count(distinct p.country) from profiles p join users u on u.id = p.user_id where u.status = 'ACTIVE'", Long.class));
        m.put("activities", jdbc.queryForObject("select count(*) from learning_activities where status in ('OPEN','COMPLETED')", Long.class));
        m.put("projects", jdbc.queryForObject("select count(*) from impact_projects where status <> 'DRAFT'", Long.class));
        return m;
    }
}
