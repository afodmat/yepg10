package com.yep.platform.team;

import com.yep.platform.achievement.AchievementService;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.messaging.ConversationService;
import com.yep.platform.notification.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.team.TeamDtos.*;
import com.yep.platform.team.TeamRepositories.*;
import com.yep.platform.user.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
    private final TeamRepository teams;
    private final MemberRepository members;
    private final InvitationRepository invitations;
    private final UserDirectory directory;
    private final ConversationService conversations;
    private final NotificationService notifications;
    private final AchievementService achievements;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public TeamService(TeamRepository teams, MemberRepository members, InvitationRepository invitations, UserDirectory directory, ConversationService conversations,
                       NotificationService notifications, AchievementService achievements, AuditService audit, JdbcTemplate jdbc) {
        this.teams = teams; this.members = members; this.invitations = invitations; this.directory = directory; this.conversations = conversations;
        this.notifications = notifications; this.achievements = achievements; this.audit = audit; this.jdbc = jdbc;
    }

    private static boolean staff(AuthUser u) { return u.role() == Role.FACILITATOR || u.role() == Role.ADMIN; }
    private int maxMembers() {
        var v = jdbc.queryForList("select setting_value from platform_settings where setting_key = 'team.max_members'", String.class);
        try { return v.isEmpty() ? 6 : Integer.parseInt(v.get(0)); } catch (NumberFormatException e) { return 6; }
    }
    private boolean inActiveTeam(Long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from team_members m join teams t on t.id = m.team_id where m.user_id = ? and t.status = 'ACTIVE')", Boolean.class, userId));
    }

    // ----- queries -----
    @Transactional(readOnly = true)
    public PageResponse<TeamView> list(String q, boolean mine, int page, int size, AuthUser me) {
        var pr = PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = mine ? teams.mine(q, me.id(), pr) : teams.browse(q, staff(me), pr);
        var names = directory.names(p.getContent().stream().map(t -> t.leaderId).toList());
        return PageResponse.of(p, t -> summary(t, names.get(t.leaderId), me));
    }

    @Transactional(readOnly = true)
    public TeamView get(Long id, AuthUser me) {
        var t = teams.findById(id).orElseThrow(() -> ApiException.notFound("Team not found"));
        boolean isMember = members.findByTeamIdAndUserId(id, me.id()).isPresent();
        if (!isMember && !staff(me) && !(t.open && "ACTIVE".equals(t.status)) && !isMentorOf(me.id(), id)) throw ApiException.notFound("Team not found");
        return detail(t, me);
    }

    /** The user's current active team, with members and project, for the dashboard. */
    @Transactional(readOnly = true)
    public Optional<TeamView> myTeam(AuthUser me) {
        return members.findByUserId(me.id()).stream().map(m -> teams.findById(m.teamId)).flatMap(Optional::stream)
                .filter(t -> "ACTIVE".equals(t.status)).findFirst().map(t -> detail(t, me));
    }

    // ----- commands -----
    @Transactional
    public TeamView create(TeamRequest r, AuthUser me, String ip) {
        if (me.role() != Role.PARTICIPANT) throw ApiException.forbidden("Only participants can create teams.");
        if (inActiveTeam(me.id())) throw ApiException.conflict("You are already in a team. Leave it before creating another.");
        var t = new Team();
        t.name = r.name().trim(); t.description = r.description(); t.country = r.country(); t.institution = r.institution(); t.category = r.category();
        t.open = r.open() == null || r.open(); t.leaderId = me.id();
        teams.save(t);
        addMember(t, me.id(), "LEADER", "Team leader");
        conversations.ensureTeamConversation(t.id);
        conversations.addToTeam(t.id, me.id());
        achievements.evaluate(me.id());
        audit.record(null, me.email(), "TEAM_CREATED", "Team", String.valueOf(t.id), Map.of("name", t.name), ip);
        return detail(t, me);
    }

    @Transactional
    public TeamView update(Long id, TeamRequest r, String status, AuthUser me) {
        var t = led(id, me);
        t.name = r.name().trim(); t.description = r.description(); t.country = r.country(); t.institution = r.institution(); t.category = r.category();
        if (r.open() != null) t.open = r.open();
        if (status != null && staff(me) && (status.equals("ACTIVE") || status.equals("ARCHIVED"))) t.status = status;
        return detail(t, me);
    }

    @Transactional
    public TeamView join(Long id, AuthUser me) {
        var t = teams.findById(id).orElseThrow(() -> ApiException.notFound("Team not found"));
        if (me.role() != Role.PARTICIPANT) throw ApiException.forbidden("Only participants can join teams.");
        if (!t.open || !"ACTIVE".equals(t.status)) throw ApiException.conflict("This team is not accepting new members. Ask the team leader for an invitation.");
        enter(t, me.id());
        return detail(t, me);
    }

    @Transactional
    public void leave(Long id, AuthUser me) {
        var t = teams.findById(id).orElseThrow(() -> ApiException.notFound("Team not found"));
        var m = members.findByTeamIdAndUserId(id, me.id()).orElseThrow(() -> ApiException.notFound("You are not in this team."));
        members.delete(m);
        conversations.removeFromTeam(id, me.id());
        var rest = members.findByTeamIdOrderByJoinedAt(id);
        if (rest.isEmpty()) { t.status = "ARCHIVED"; return; }
        if (t.leaderId.equals(me.id())) {                       // hand leadership to the longest-standing member
            var next = rest.get(0);
            next.memberRole = "LEADER"; t.leaderId = next.userId;
            jdbc.update("update impact_projects set leader_id = ? where team_id = ?", next.userId, id);
            notifications.notify(next.userId, NotificationType.TEAM_INVITATION, "You now lead " + t.name, "The previous leader left, so you are the new team leader.", "team");
        }
    }

    @Transactional
    public void removeMember(Long id, Long userId, AuthUser me) {
        var t = led(id, me);
        if (t.leaderId.equals(userId)) throw ApiException.badRequest("The team leader cannot be removed. Transfer leadership by leaving the team.");
        var m = members.findByTeamIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Member not found"));
        members.delete(m);
        conversations.removeFromTeam(id, userId);
        notifications.notify(userId, NotificationType.TEAM_INVITATION, "Removed from " + t.name, "You are no longer a member of this team.", "team");
    }

    @Transactional
    public void setTitle(Long id, Long userId, String title, AuthUser me) {
        led(id, me);
        members.findByTeamIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Member not found")).title = title;
    }

    @Transactional
    public void invite(Long id, String email, AuthUser me) {
        var t = led(id, me);
        email = email.trim().toLowerCase();
        if (members.countByTeamId(id) >= maxMembers()) throw ApiException.conflict("This team is full.");
        if (invitations.pendingExists(id, email)) throw ApiException.conflict("An invitation is already pending for that email.");
        var existing = directory.byEmail(email);
        if (existing.isPresent() && members.findByTeamIdAndUserId(id, existing.get().getId()).isPresent()) throw ApiException.conflict("That person is already in the team.");
        var inv = new TeamInvitation();
        inv.teamId = id; inv.email = email; inv.invitedBy = me.id();
        invitations.save(inv);
        existing.ifPresent(u -> notifications.notify(u.getId(), NotificationType.TEAM_INVITATION, "Invitation to join " + t.name, directory.name(me.id()) + " invited you to their team.", "team"));
    }

    @Transactional(readOnly = true)
    public List<InvitationView> myInvitations(AuthUser me) {
        var invs = invitations.pendingFor(me.email());
        var names = directory.names(invs.stream().map(i -> i.invitedBy).toList());
        Map<Long, String> teamNames = new HashMap<>();
        teams.findAllById(invs.stream().map(i -> i.teamId).toList()).forEach(t -> teamNames.put(t.id, t.name));
        return invs.stream().map(i -> new InvitationView(i.id, i.teamId, teamNames.get(i.teamId), names.get(i.invitedBy), i.createdAt)).toList();
    }

    @Transactional
    public TeamView respond(Long invitationId, boolean accept, AuthUser me) {
        var inv = invitations.findById(invitationId).filter(i -> i.email.equalsIgnoreCase(me.email()) && "PENDING".equals(i.status))
                .orElseThrow(() -> ApiException.notFound("Invitation not found"));
        var t = teams.findById(inv.teamId).orElseThrow(() -> ApiException.notFound("Team not found"));
        inv.status = accept ? "ACCEPTED" : "DECLINED";
        if (!accept) return null;
        if (me.role() != Role.PARTICIPANT) throw ApiException.forbidden("Only participants can join teams.");
        enter(t, me.id());
        notifications.notify(t.leaderId, NotificationType.TEAM_INVITATION, "New teammate", directory.name(me.id()) + " accepted your invitation to " + t.name + ".", "team");
        return detail(t, me);
    }

    // ----- helpers -----
    private void enter(Team t, Long userId) {
        if (members.findByTeamIdAndUserId(t.id, userId).isPresent()) throw ApiException.conflict("You are already in this team.");
        if (inActiveTeam(userId)) throw ApiException.conflict("You are already in a team. Leave it before joining another.");
        if (members.countByTeamId(t.id) >= maxMembers()) throw ApiException.conflict("This team is full.");
        addMember(t, userId, "MEMBER", null);
        conversations.addToTeam(t.id, userId);
        achievements.evaluate(userId);
    }
    private void addMember(Team t, Long userId, String role, String title) {
        var m = new TeamMember();
        m.teamId = t.id; m.userId = userId; m.memberRole = role; m.title = title;
        members.save(m);
    }
    private Team led(Long id, AuthUser me) {
        var t = teams.findById(id).orElseThrow(() -> ApiException.notFound("Team not found"));
        if (!staff(me) && !t.leaderId.equals(me.id())) throw ApiException.forbidden("Only the team leader can do this.");
        return t;
    }
    private boolean isMentorOf(Long userId, Long teamId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from mentorship_assignments a where a.mentor_id = ? and a.status = 'ACTIVE' and (a.team_id = ? or a.participant_id in (select user_id from team_members where team_id = ?)))", Boolean.class, userId, teamId, teamId));
    }

    private TeamView summary(Team t, String leaderName, AuthUser me) {
        boolean member = members.findByTeamIdAndUserId(t.id, me.id()).isPresent();
        return new TeamView(t.id, t.name, t.description, t.leaderId, leaderName, t.country, t.institution, t.category, t.open, t.status, members.countByTeamId(t.id), member, t.getCreatedAt(), null, null, null);
    }

    private TeamView detail(Team t, AuthUser me) {
        var ms = members.findByTeamIdOrderByJoinedAt(t.id);
        var memberIds = ms.stream().map(m -> m.userId).toList();
        var names = directory.names(memberIds);
        var countries = directory.countries(memberIds);
        var mv = ms.stream().map(m -> new MemberView(m.userId, names.get(m.userId), countries.get(m.userId), m.memberRole, m.title, m.joinedAt)).toList();
        var projects = jdbc.query("select id, name, progress_pct, status from impact_projects where team_id = ?", (rs, i) -> new ProjectSummary(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getString(4)), t.id);
        var mentorIds = jdbc.queryForList("select distinct mentor_id from mentorship_assignments where status = 'ACTIVE' and (team_id = ? or participant_id in (select user_id from team_members where team_id = ?))", Long.class, t.id, t.id);
        var mentorNames = new ArrayList<>(directory.names(mentorIds).values());
        boolean member = ms.stream().anyMatch(m -> m.userId.equals(me.id()));
        return new TeamView(t.id, t.name, t.description, t.leaderId, names.get(t.leaderId), t.country, t.institution, t.category, t.open, t.status, ms.size(), member, t.getCreatedAt(),
                mv, projects.isEmpty() ? null : projects.get(0), mentorNames);
    }
}
