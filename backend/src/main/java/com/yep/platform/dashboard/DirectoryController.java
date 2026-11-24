package com.yep.platform.dashboard;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Staff-only people picker (e.g. to assign a mentor). Returns at most 10 matches and never exposes contact details beyond email. */
@RestController @RequestMapping("/api/directory")
@PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
public class DirectoryController {
    public record Person(Long id, String name, String email) {}
    private final JdbcTemplate jdbc;
    public DirectoryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/participants")
    public List<Person> participants(@RequestParam(defaultValue = "") String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        return jdbc.query("select u.id, p.first_name || ' ' || p.last_name, u.email from users u join profiles p on p.user_id = u.id where u.role = 'PARTICIPANT' and u.status = 'ACTIVE' and (lower(u.email) like ? or lower(p.first_name || ' ' || p.last_name) like ?) order by p.first_name limit 10",
                (rs, i) -> new Person(rs.getLong(1), rs.getString(2), rs.getString(3)), like, like);
    }
}
