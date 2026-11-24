package com.yep.platform.project;

import com.yep.platform.achievement.AchievementService;
import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.notification.*;
import com.yep.platform.project.ProjectDtos.*;
import com.yep.platform.project.ProjectRepositories.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private static final List<String> MILESTONES = List.of("Research", "Planning", "Prototype", "Testing", "Implementation", "Final Submission");

    private final ProjectRepository projects;
    private final MilestoneRepository milestones;
    private final UpdateRepository updates;
    private final CommentRepository comments;
    private final UserDirectory directory;
    private final NotificationService notifications;
    private final AchievementService achievements;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public ProjectService(ProjectRepository projects, MilestoneRepository milestones, UpdateRepository updates, CommentRepository comments, UserDirectory directory,
                          NotificationService notifications, AchievementService achievements, AuditService audit, JdbcTemplate jdbc) {
        this.projects = projects; this.milestones = milestones; this.updates = updates; this.comments = comments; this.directory = directory;
        this.notifications = notifications; this.achievements = achievements; this.audit = audit; this.jdbc = jdbc;
    }

    private static boolean staff(AuthUser u) { return u != null && (u.role() == Role.FACILITATOR || u.role() == Role.ADMIN); }

    // ----- access rules -----
    private boolean isMember(Long userId, Long teamId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from team_members where team_id = ? and user_id = ?)", Boolean.class, teamId, userId));
    }
    private boolean isMentor(Long userId, Long teamId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from mentorship_assignments a where a.mentor_id = ? and a.status = 'ACTIVE' and (a.team_id = ? or a.participant_id in (select user_id from team_members where team_id = ?)))", Boolean.class, userId, teamId, teamId));
    }

    // ----- queries -----
    @Transactional(readOnly = true)
    public PageResponse<ProjectView> list(String q, String category, boolean mine, int page, int size, AuthUser me) {
        var pr = PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (mine && me == null) throw ApiException.unauthorized("Please log in to continue.");
        var p = mine ? projects.mine(me.id(), pr) : projects.browse(q == null ? "" : q.trim(), category == null ? "" : category, pr);
        return PageResponse.of(p, pr0 -> view(pr0, me, false));
    }

    @Transactional(readOnly = true)
    public ProjectView get(Long id, AuthUser me) { return view(find(id), me, true); }

    // ----- commands -----
    @Transactional
    public ProjectView create(ProjectRequest r, AuthUser me, String ip) {
        if (r.teamId() == null) throw ApiException.badRequest("Choose the team this project belongs to.");
        var leaderCheck = jdbc.queryForList("select leader_id from teams where id = ? and status = 'ACTIVE'", Long.class, r.teamId());
        if (leaderCheck.isEmpty() || !leaderCheck.get(0).equals(me.id())) throw ApiException.forbidden("Only the team leader can start the team's project.");
        if (projects.findByTeamId(r.teamId()).isPresent()) throw ApiException.conflict("This team already has an Impact Project.");
        validateDates(r);
        var p = new ImpactProject();
        p.teamId = r.teamId(); p.leaderId = me.id();
        fill(p, r);
        projects.save(p);
        short pos = 0;
        for (String name : MILESTONES) { var m = new ProjectMilestone(); m.projectId = p.id; m.name = name; m.position = pos++; milestones.save(m); }
        achievements.evaluate(me.id());
        audit.record(null, me.email(), "PROJECT_CREATED", "ImpactProject", String.valueOf(p.id), Map.of("name", p.name), ip);
        return view(p, me, true);
    }

    @Transactional
    public ProjectView update(Long id, ProjectRequest r, AuthUser me, String ip) {
        var p = find(id);
        if (!staff(me) && !p.leaderId.equals(me.id())) throw ApiException.forbidden("Only the project leader can edit project details.");
        validateDates(r);
        fill(p, r);
        audit.record(null, me.email(), "PROJECT_UPDATED", "ImpactProject", String.valueOf(id), Map.of("name", p.name), ip);
        return view(p, me, true);
    }

    @Transactional
    public ProjectView setStatus(Long id, String status, AuthUser me, String ip) {
        if (!staff(me)) throw ApiException.forbidden("Only facilitators and admins can change the project status.");
        var p = find(id);
        p.status = status;
        audit.record(null, me.email(), "PROJECT_STATUS_CHANGED", "ImpactProject", String.valueOf(id), Map.of("status", status), ip);
        notifyTeam(p, me.id(), "Project status: " + status, p.name + " is now " + status.toLowerCase() + ".");
        return view(p, me, true);
    }

    @Transactional
    public ProjectView postUpdate(Long id, UpdateRequest r, AuthUser me) {
        var p = find(id);
        requireContributor(p, me);
        var u = new ProjectPost.Update();
        u.projectId = id; u.authorId = me.id(); u.content = r.content().trim();
        updates.save(u);
        if (r.milestoneId() != null && r.milestoneStatus() != null) setMilestone(p, r.milestoneId(), r.milestoneStatus());
        achievements.evaluate(me.id());
        notifyReviewers(p, me.id(), "Project update: " + p.name, directory.name(me.id()) + " posted a progress update.");
        return view(p, me, true);
    }

    @Transactional
    public ProjectView patchMilestone(Long id, Long milestoneId, String status, AuthUser me) {
        var p = find(id);
        requireContributor(p, me);
        setMilestone(p, milestoneId, status);
        return view(p, me, true);
    }

    @Transactional
    public ProjectView comment(Long id, String content, AuthUser me) {
        var p = find(id);
        if (!isMember(me.id(), p.teamId) && !isMentor(me.id(), p.teamId) && !staff(me)) throw ApiException.forbidden("Only the team, its mentors and facilitators can comment.");
        var c = new ProjectPost.Comment();
        c.projectId = id; c.authorId = me.id(); c.content = content.trim();
        comments.save(c);
        achievements.evaluate(me.id());
        notifyTeam(p, me.id(), "New comment on " + p.name, directory.name(me.id()) + " left feedback on your project.");
        return view(p, me, true);
    }

    @Transactional
    public ProjectView attach(Long id, Long fileId, AuthUser me) {
        var p = find(id);
        requireContributor(p, me);
        var ok = jdbc.queryForList("select 1 from files where id = ? and owner_id = ? and purpose in ('PROJECT_DOC','PROJECT_IMAGE')", Integer.class, fileId, me.id());
        if (ok.isEmpty()) throw ApiException.badRequest("Upload the file as a project document or image first.");
        jdbc.update("insert into project_attachments (project_id, file_id) values (?, ?) on conflict do nothing", id, fileId);
        return view(p, me, true);
    }

    // ----- helpers -----
    private ImpactProject find(Long id) { return projects.findById(id).orElseThrow(() -> ApiException.notFound("Project not found")); }

    private void requireContributor(ImpactProject p, AuthUser me) {
        if (!isMember(me.id(), p.teamId) && !staff(me)) throw ApiException.forbidden("Only team members can update the project.");
    }
    private static void validateDates(ProjectRequest r) {
        if (r.startDate() != null && r.targetDate() != null && r.targetDate().isBefore(r.startDate())) throw ApiException.badRequest("Target date must be on or after the start date.");
    }
    private static void fill(ImpactProject p, ProjectRequest r) {
        p.name = r.name().trim(); p.description = r.description().trim(); p.problemStatement = r.problemStatement(); p.solution = r.solution();
        p.category = r.category(); p.location = r.location(); p.startDate = r.startDate(); p.targetDate = r.targetDate();
    }

    private void setMilestone(ImpactProject p, Long milestoneId, String status) {
        var m = milestones.findById(milestoneId).filter(x -> x.projectId.equals(p.id)).orElseThrow(() -> ApiException.notFound("Milestone not found"));
        m.status = status;
        m.completedAt = status.equals("DONE") ? Instant.now() : null;
        milestones.flush();
        var all = milestones.findByProjectIdOrderByPosition(p.id);
        long done = all.stream().filter(x -> x.status.equals("DONE")).count();
        p.progressPct = (short) (all.isEmpty() ? 0 : done * 100 / all.size());
        if (done == all.size() && p.status.equals("ACTIVE")) {
            p.status = "SUBMITTED";
            notifyReviewers(p, null, "Project submitted: " + p.name, "All milestones are complete and the project is ready for review.");
        } else if (done < all.size() && p.status.equals("SUBMITTED")) p.status = "ACTIVE";
    }

    private void notifyTeam(ImpactProject p, Long exceptUserId, String title, String body) {
        jdbc.queryForList("select user_id from team_members where team_id = ?", Long.class, p.teamId).stream().filter(u -> !u.equals(exceptUserId))
                .forEach(u -> notifications.notify(u, NotificationType.PROJECT_UPDATE, title, body, "team"));
    }
    private void notifyReviewers(ImpactProject p, Long exceptUserId, String title, String body) {
        var mentors = jdbc.queryForList("select distinct mentor_id from mentorship_assignments a where a.status = 'ACTIVE' and (a.team_id = ? or a.participant_id in (select user_id from team_members where team_id = ?))", Long.class, p.teamId, p.teamId);
        mentors.stream().filter(u -> !u.equals(exceptUserId)).forEach(u -> notifications.notify(u, NotificationType.PROJECT_UPDATE, title, body, "mentees"));
        if (!p.leaderId.equals(exceptUserId)) notifications.notify(p.leaderId, NotificationType.PROJECT_UPDATE, title, body, "team");
    }

    /** Summaries are public; milestones, updates, comments and files are for the team, its mentors and staff only. */
    private ProjectView view(ImpactProject p, AuthUser me, boolean detail) {
        boolean member = me != null && isMember(me.id(), p.teamId);
        boolean mentor = me != null && isMentor(me.id(), p.teamId);
        boolean canView = member || mentor || staff(me);
        var teamName = jdbc.queryForList("select name from teams where id = ?", String.class, p.teamId).stream().findFirst().orElse(null);
        var mentorNames = new ArrayList<>(directory.names(jdbc.queryForList("select distinct mentor_id from mentorship_assignments a where a.status = 'ACTIVE' and (a.team_id = ? or a.participant_id in (select user_id from team_members where team_id = ?))", Long.class, p.teamId, p.teamId)).values());
        List<MilestoneView> ms = List.of(); List<PostView> ups = List.of(), cms = List.of(); List<AttachmentView> atts = List.of();
        if (detail && canView) {
            ms = milestones.findByProjectIdOrderByPosition(p.id).stream().map(m -> new MilestoneView(m.id, m.name, m.status, m.completedAt)).toList();
            var uList = updates.findTop30ByProjectIdOrderByCreatedAtDesc(p.id);
            var cList = comments.findTop50ByProjectIdOrderByCreatedAtDesc(p.id);
            List<Long> authorIds = new ArrayList<>();
            uList.forEach(u -> authorIds.add(u.authorId));
            cList.forEach(c -> authorIds.add(c.authorId));
            var names = directory.names(authorIds);
            ups = uList.stream().map(u -> new PostView(u.id, u.authorId, names.get(u.authorId), u.content, u.createdAt)).toList();
            cms = cList.stream().map(c -> new PostView(c.id, c.authorId, names.get(c.authorId), c.content, c.createdAt)).toList();
            atts = jdbc.query("select f.id, f.original_name, f.content_type from project_attachments a join files f on f.id = a.file_id where a.project_id = ? order by f.id", (rs, i) -> new AttachmentView(rs.getLong(1), rs.getString(2), rs.getString(3)), p.id);
        } else if (!detail) {
            ms = milestones.findByProjectIdOrderByPosition(p.id).stream().map(m -> new MilestoneView(m.id, m.name, m.status, m.completedAt)).toList();
        }
        return new ProjectView(p.id, p.teamId, teamName, p.name, p.description, canView ? p.problemStatement : null, canView ? p.solution : null, p.category, p.location,
                p.leaderId, directory.name(p.leaderId), p.status, p.progressPct, p.startDate, p.targetDate, mentorNames,
                canView, me != null && (staff(me) || p.leaderId.equals(me.id())), member || staff(me), mentor || staff(me), ms, ups, cms, atts);
    }
}
