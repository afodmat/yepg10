package com.yep.platform.admin;

import com.yep.platform.achievement.*;
import com.yep.platform.achievement.AchievementRepositories.AchievementRepository;
import com.yep.platform.admin.AdminDtos.*;
import com.yep.platform.audit.AuditService;
import com.yep.platform.auth.SessionRevoker;
import com.yep.platform.common.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private static final Map<String, String> SORTS = Map.of("name", "p.first_name", "email", "u.email", "role", "u.role", "status", "u.status", "created", "u.created_at", "login", "u.last_login_at");

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final SessionRevoker revoker;
    private final AuditService audit;
    private final AchievementRepository achievements;
    private final AchievementService achievementService;

    public AdminService(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, UserRepository users, ProfileRepository profiles, PasswordEncoder encoder, SessionRevoker revoker,
                        AuditService audit, AchievementRepository achievements, AchievementService achievementService) {
        this.jdbc = jdbc; this.named = named; this.users = users; this.profiles = profiles; this.encoder = encoder; this.revoker = revoker;
        this.audit = audit; this.achievements = achievements; this.achievementService = achievementService;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("totalUsers", count("select count(*) from users"));
        m.put("activeParticipants", count("select count(*) from users where role = 'PARTICIPANT' and status = 'ACTIVE'"));
        m.put("mentors", count("select count(*) from users where role = 'MENTOR' and status = 'ACTIVE'"));
        m.put("facilitators", count("select count(*) from users where role = 'FACILITATOR' and status = 'ACTIVE'"));
        m.put("learningActivities", count("select count(*) from learning_activities where status <> 'DRAFT'"));
        m.put("enrollments", count("select count(*) from enrollments where status <> 'CANCELLED'"));
        m.put("activeProjects", count("select count(*) from impact_projects where status in ('ACTIVE','SUBMITTED')"));
        m.put("completedProjects", count("select count(*) from impact_projects where status = 'COMPLETED'"));
        m.put("exchangeSessions", count("select count(*) from exchange_sessions where status <> 'CANCELLED'"));
        m.put("certificatesIssued", count("select count(*) from certificates where status = 'VALID'"));
        m.put("teams", count("select count(*) from teams where status = 'ACTIVE'"));
        m.put("newUsersLast7Days", count("select count(*) from users where created_at > now() - interval '7 days'"));
        return m;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserRow> users(String q, String role, String status, String sort, String dir, int page, int size) {
        var where = new StringBuilder(" where 1=1");
        var params = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) { where.append(" and (lower(u.email) like :q or lower(p.first_name || ' ' || p.last_name) like :q)"); params.addValue("q", "%" + q.trim().toLowerCase() + "%"); }
        if (role != null && !role.isBlank()) { where.append(" and u.role = :role"); params.addValue("role", role); }
        if (status != null && !status.isBlank()) { where.append(" and u.status = :status"); params.addValue("status", status); }
        String from = " from users u join profiles p on p.user_id = u.id" + where;
        long total = named.queryForObject("select count(*)" + from, params, Long.class);
        int sz = Math.min(Math.max(size, 1), 50), pg = Math.max(page, 0);
        String order = SORTS.getOrDefault(sort, "u.created_at") + ("asc".equalsIgnoreCase(dir) ? " asc" : " desc");   // whitelist: never concatenates user input
        params.addValue("limit", sz).addValue("offset", pg * sz);
        var rows = named.query("select u.id, u.email, p.first_name, p.last_name, u.role, u.status, p.country, u.created_at, u.last_login_at" + from + " order by " + order + " limit :limit offset :offset", params,
                (rs, i) -> new UserRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), Role.valueOf(rs.getString(5)), UserStatus.valueOf(rs.getString(6)), rs.getString(7),
                        rs.getTimestamp(8).toInstant(), rs.getTimestamp(9) == null ? null : rs.getTimestamp(9).toInstant()));
        return new PageResponse<>(rows, pg, sz, total, (int) Math.ceil(total / (double) sz));
    }

    @Transactional
    public UserRow createUser(CreateUserRequest r, AuthUser admin, String ip) {
        String email = r.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) throw ApiException.conflict("A user with that email already exists.");
        var u = users.save(new User(email, encoder.encode(r.temporaryPassword()), r.role(), UserStatus.ACTIVE));
        profiles.save(new Profile(u, r.firstName().trim(), r.lastName().trim(), null, r.country().trim(), null));
        audit.record(null, admin.email(), "USER_CREATED", "User", String.valueOf(u.getId()), Map.of("role", r.role().name()), ip);
        return new UserRow(u.getId(), email, r.firstName(), r.lastName(), r.role(), UserStatus.ACTIVE, r.country(), java.time.Instant.now(), null);
    }

    @Transactional
    public void setRole(Long id, Role role, AuthUser admin, String ip) {
        if (id.equals(admin.id())) throw ApiException.badRequest("You cannot change your own role.");
        var u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        var old = u.getRole();
        u.setRole(role);
        revoker.revokeAll(id);   // forces re-login so the new role is in effect on the next token
        audit.record(null, admin.email(), "ROLE_CHANGED", "User", String.valueOf(id), Map.of("from", old.name(), "to", role.name()), ip);
    }

    @Transactional
    public void setStatus(Long id, UserStatus status, AuthUser admin, String ip) {
        if (id.equals(admin.id())) throw ApiException.badRequest("You cannot change your own status.");
        var u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setStatus(status);
        if (status == UserStatus.DISABLED) revoker.revokeAll(id);
        audit.record(null, admin.email(), status == UserStatus.DISABLED ? "USER_DISABLED" : "USER_ENABLED", "User", String.valueOf(id), Map.of("email", u.getEmail()), ip);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditRow> auditLogs(String action, int page, int size) {
        int sz = Math.min(Math.max(size, 1), 50), pg = Math.max(page, 0);
        var params = new MapSqlParameterSource("limit", sz).addValue("offset", pg * sz);
        String where = "";
        if (action != null && !action.isBlank()) { where = " where action = :action"; params.addValue("action", action); }
        long total = named.queryForObject("select count(*) from audit_logs" + where, params, Long.class);
        var rows = named.query("select id, actor_email, action, entity_type, entity_id, metadata::text, created_at from audit_logs" + where + " order by id desc limit :limit offset :offset", params,
                (rs, i) -> new AuditRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), parse(rs.getString(6)), rs.getTimestamp(7).toInstant()));
        return new PageResponse<>(rows, pg, sz, total, (int) Math.ceil(total / (double) sz));
    }

    @Transactional(readOnly = true)
    public Map<String, String> settings() {
        Map<String, String> out = new TreeMap<>();
        jdbc.query("select setting_key, setting_value from platform_settings", rs -> { out.put(rs.getString(1), rs.getString(2)); });
        return out;
    }

    @Transactional
    public void updateSettings(Map<String, String> changes, AuthUser admin, String ip) {
        var known = settings();
        for (var e : changes.entrySet()) {
            if (!known.containsKey(e.getKey())) throw ApiException.badRequest("Unknown setting: " + e.getKey());
            if (e.getValue() == null || e.getValue().isBlank() || e.getValue().length() > 500) throw ApiException.badRequest("Invalid value for " + e.getKey());
            if (e.getKey().equals("team.max_members") && !e.getValue().matches("\\d{1,2}")) throw ApiException.badRequest("team.max_members must be a number");
            jdbc.update("update platform_settings set setting_value = ?, updated_at = now() where setting_key = ?", e.getValue(), e.getKey());
        }
        audit.record(null, admin.email(), "SETTINGS_CHANGED", "PlatformSettings", null, Map.of("keys", String.join(",", changes.keySet())), ip);
    }

    @Transactional
    public int broadcast(BroadcastRequest r, AuthUser admin, String ip) {
        int n = r.role() == null
                ? jdbc.update("insert into notifications (user_id, type, title, body) select id, 'SYSTEM', ?, ? from users where status = 'ACTIVE'", r.title(), r.body())
                : jdbc.update("insert into notifications (user_id, type, title, body) select id, 'SYSTEM', ?, ? from users where status = 'ACTIVE' and role = ?", r.title(), r.body(), r.role().name());
        audit.record(null, admin.email(), "NOTIFICATION_BROADCAST", "Notification", null, Map.of("recipients", n, "title", r.title()), ip);
        return n;
    }

    @Transactional(readOnly = true) public List<Achievement> allAchievements() { return achievements.findAll(); }

    @Transactional
    public Achievement saveAchievement(Long id, AchievementRequest r, AuthUser admin, String ip) {
        var a = id == null ? new Achievement() : achievements.findById(id).orElseThrow(() -> ApiException.notFound("Achievement not found"));
        if (id == null && achievements.findByCode(r.code()).isPresent()) throw ApiException.conflict("An achievement with that code already exists.");
        if (id == null) a.code = r.code().trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        a.name = r.name().trim(); a.description = r.description().trim(); a.icon = r.icon().trim(); a.criteria = r.criteria().trim(); a.active = r.active();
        achievements.save(a);
        audit.record(null, admin.email(), id == null ? "ACHIEVEMENT_CREATED" : "ACHIEVEMENT_UPDATED", "Achievement", String.valueOf(a.id), Map.of("code", a.code), ip);
        return a;
    }

    @Transactional
    public void awardManually(Long userId, Long achievementId, AuthUser admin, String ip) {
        var a = achievements.findById(achievementId).orElseThrow(() -> ApiException.notFound("Achievement not found"));
        if (!users.existsById(userId)) throw ApiException.notFound("User not found");
        achievementService.award(userId, a);
        audit.record(null, admin.email(), "ACHIEVEMENT_AWARDED", "Achievement", String.valueOf(achievementId), Map.of("userId", userId), ip);
    }

    private long count(String sql) { Long n = jdbc.queryForObject(sql, Long.class); return n == null ? 0 : n; }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        if (json == null) return null;
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }
}
