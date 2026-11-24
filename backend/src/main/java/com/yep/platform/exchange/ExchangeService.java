package com.yep.platform.exchange;

import com.yep.platform.achievement.AchievementService;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.exchange.ExchangeDtos.*;
import com.yep.platform.exchange.ExchangeRepositories.*;
import com.yep.platform.notification.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeService {
    /** A session counts as "upcoming" until this long after it starts. */
    private static final Duration LIVE_WINDOW = Duration.ofHours(3);

    private final SessionRepository sessions;
    private final ParticipantRepository participants;
    private final ProfileRepository profiles;
    private final UserDirectory directory;
    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationService notifications;
    private final AchievementService achievements;
    private final AuditService audit;

    public ExchangeService(SessionRepository sessions, ParticipantRepository participants, ProfileRepository profiles, UserDirectory directory,
                           NamedParameterJdbcTemplate jdbc, NotificationService notifications, AchievementService achievements, AuditService audit) {
        this.sessions = sessions; this.participants = participants; this.profiles = profiles; this.directory = directory;
        this.jdbc = jdbc; this.notifications = notifications; this.achievements = achievements; this.audit = audit;
    }

    private static boolean staff(AuthUser u) { return u != null && (u.role() == Role.FACILITATOR || u.role() == Role.ADMIN); }

    @Transactional(readOnly = true)
    public PageResponse<SessionView> search(String q, String when, int page, int size, AuthUser me) {
        String w = when == null ? "" : when;
        Sort sort = w.equals("completed") ? Sort.by(Sort.Direction.DESC, "startsAt") : Sort.by(Sort.Direction.ASC, "startsAt");
        var p = sessions.search(q == null ? "" : q.trim(), w, Instant.now().minus(LIVE_WINDOW), staff(me), PageResponse.request(page, size, sort));
        return toViews(p, me);
    }

    @Transactional(readOnly = true)
    public SessionView get(Long id, AuthUser me) {
        var s = sessions.findById(id).orElseThrow(() -> ApiException.notFound("Session not found"));
        return views(List.of(s), me).get(0);
    }

    /** Sessions the user registered for that have not finished yet (dashboard). */
    @Transactional(readOnly = true)
    public List<SessionView> upcomingFor(Long userId, AuthUser me) {
        var ids = jdbc.queryForList("select s.id from exchange_sessions s join exchange_participants p on p.session_id = s.id where p.user_id = :u and s.status = 'SCHEDULED' and s.starts_at >= :c order by s.starts_at limit 3",
                new MapSqlParameterSource("u", userId).addValue("c", java.sql.Timestamp.from(Instant.now().minus(LIVE_WINDOW))), Long.class);
        return views(sessions.findAllById(ids), me);
    }

    @Transactional
    public SessionView register(Long id, AuthUser me) {
        var s = sessions.findById(id).orElseThrow(() -> ApiException.notFound("Session not found"));
        if (s.status != ExchangeStatus.SCHEDULED || s.startsAt.isBefore(Instant.now().minus(LIVE_WINDOW))) throw ApiException.conflict("Registration is closed for this session.");
        if (participants.findBySessionIdAndUserId(id, me.id()).isPresent()) throw ApiException.conflict("You are already registered for this session.");
        var p = new ExchangeParticipant();
        p.sessionId = id; p.userId = me.id(); p.country = profiles.findById(me.id()).orElseThrow().getCountry();
        participants.save(p);
        notifications.notify(me.id(), NotificationType.LEARNING_ACTIVITY, "Registered: " + s.title, "You're in! The meeting link is on the session page.", "exchange");
        achievements.evaluate(me.id());
        return views(List.of(s), me).get(0);
    }

    @Transactional
    public void unregister(Long id, AuthUser me) {
        var p = participants.findBySessionIdAndUserId(id, me.id()).orElseThrow(() -> ApiException.notFound("You are not registered for this session."));
        participants.delete(p);
    }

    @Transactional
    public SessionView create(SessionRequest r, AuthUser me, String ip) {
        var s = new ExchangeSession();
        apply(s, r);
        s.facilitatorId = me.id();
        sessions.save(s);
        audit.record(null, me.email(), "SESSION_CREATED", "ExchangeSession", String.valueOf(s.id), Map.of("title", s.title), ip);
        return views(List.of(s), me).get(0);
    }

    @Transactional
    public SessionView update(Long id, SessionRequest r, AuthUser me, String ip) {
        var s = owned(id, me);
        var wasCancelled = s.status == ExchangeStatus.CANCELLED;
        apply(s, r);
        if (!wasCancelled && s.status == ExchangeStatus.CANCELLED)
            participants.findBySessionId(id).forEach(p -> notifications.notify(p.userId, NotificationType.LEARNING_ACTIVITY, "Session cancelled", s.title + " has been cancelled.", "exchange"));
        audit.record(null, me.email(), "SESSION_UPDATED", "ExchangeSession", String.valueOf(id), Map.of("status", s.status.name()), ip);
        return views(List.of(s), me).get(0);
    }

    @Transactional
    public void delete(Long id, AuthUser me, String ip) {
        var s = owned(id, me);
        if (participants.countBySessionId(id) > 0) throw ApiException.conflict("Participants are registered. Cancel the session instead of deleting it.");
        sessions.delete(s);
        audit.record(null, me.email(), "SESSION_DELETED", "ExchangeSession", String.valueOf(id), Map.of("title", s.title), ip);
    }

    private ExchangeSession owned(Long id, AuthUser me) {
        var s = sessions.findById(id).orElseThrow(() -> ApiException.notFound("Session not found"));
        if (me.role() != Role.ADMIN && !Objects.equals(s.facilitatorId, me.id())) throw ApiException.forbidden("You can only manage sessions you facilitate.");
        return s;
    }

    private void apply(ExchangeSession s, SessionRequest r) {
        s.title = r.title().trim(); s.description = r.description().trim(); s.topic = r.topic().trim(); s.startsAt = r.startsAt();
        s.durationMinutes = r.durationMinutes(); s.meetingLink = r.meetingLink() == null || r.meetingLink().isBlank() ? null : r.meetingLink();
        s.discussionPrompts = r.discussionPrompts(); s.resources = r.resources();
        if (r.status() != null) s.status = r.status();
    }

    private PageResponse<SessionView> toViews(org.springframework.data.domain.Page<ExchangeSession> p, AuthUser me) {
        var list = views(p.getContent(), me);
        return new PageResponse<>(list, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private List<SessionView> views(Collection<ExchangeSession> list, AuthUser me) {
        if (list.isEmpty()) return List.of();
        var ids = list.stream().map(s -> s.id).toList();
        Map<Long, Long> counts = new HashMap<>();
        Map<Long, Set<String>> countries = new HashMap<>();
        jdbc.query("select session_id, country, count(*) from exchange_participants where session_id in (:ids) group by session_id, country", new MapSqlParameterSource("ids", ids), rs -> {
            counts.merge(rs.getLong(1), rs.getLong(3), Long::sum);
            countries.computeIfAbsent(rs.getLong(1), k -> new TreeSet<>()).add(rs.getString(2));
        });
        Set<Long> mine = new HashSet<>();
        if (me != null) jdbc.query("select session_id from exchange_participants where user_id = :u and session_id in (:ids)", new MapSqlParameterSource("u", me.id()).addValue("ids", ids), rs -> { mine.add(rs.getLong(1)); });
        var names = directory.names(list.stream().map(s -> s.facilitatorId).toList());
        Instant cutoff = Instant.now().minus(LIVE_WINDOW);
        return list.stream().map(s -> {
            String phase = s.status == ExchangeStatus.CANCELLED ? "CANCELLED"
                    : (s.status == ExchangeStatus.COMPLETED || s.startsAt.isBefore(cutoff)) ? "COMPLETED" : "UPCOMING";
            boolean registered = mine.contains(s.id);
            boolean seeLink = registered || (me != null && (me.role() == Role.ADMIN || me.id().equals(s.facilitatorId)));
            return new SessionView(s.id, s.title, s.description, s.topic, s.startsAt, s.durationMinutes, s.facilitatorId, names.get(s.facilitatorId), s.status.name(), phase,
                    counts.getOrDefault(s.id, 0L), new ArrayList<>(countries.getOrDefault(s.id, Set.of())), registered, seeLink ? s.meetingLink : null,
                    lines(s.discussionPrompts), seeLink ? lines(s.resources) : List.of());
        }).collect(Collectors.toList());
    }

    private static List<String> lines(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }
}
