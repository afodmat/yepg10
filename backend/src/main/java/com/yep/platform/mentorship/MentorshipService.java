package com.yep.platform.mentorship;

import com.yep.platform.audit.AuditService;
import com.yep.platform.common.*;
import com.yep.platform.messaging.ConversationService;
import com.yep.platform.mentorship.MentorshipDtos.*;
import com.yep.platform.mentorship.MentorshipRepositories.*;
import com.yep.platform.notification.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.user.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MentorshipService {
    private final MentorProfileRepository mentors;
    private final AssignmentRepository assignments;
    private final RequestRepository requests;
    private final ProfileRepository profiles;
    private final UserDirectory directory;
    private final ConversationService conversations;
    private final NotificationService notifications;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public MentorshipService(MentorProfileRepository mentors, AssignmentRepository assignments, RequestRepository requests, ProfileRepository profiles, UserDirectory directory,
                             ConversationService conversations, NotificationService notifications, AuditService audit, JdbcTemplate jdbc) {
        this.mentors = mentors; this.assignments = assignments; this.requests = requests; this.profiles = profiles; this.directory = directory;
        this.conversations = conversations; this.notifications = notifications; this.audit = audit; this.jdbc = jdbc;
    }

    // ----- mentor directory -----
    @Transactional(readOnly = true)
    public PageResponse<MentorView> listMentors(String q, int page, int size) {
        var p = mentors.search(q == null ? "" : q.trim(), PageResponse.request(page, size, Sort.by("userId")));
        var names = directory.names(p.getContent().stream().map(m -> m.userId).toList());
        Map<Long, String> countries = new HashMap<>();
        profiles.findAllById(p.getContent().stream().map(m -> m.userId).toList()).forEach(pr -> countries.put(pr.getUserId(), pr.getCountry()));
        return PageResponse.of(p, m -> {
            long active = assignments.countByMentorIdAndStatus(m.userId, "ACTIVE");
            return new MentorView(m.userId, names.get(m.userId), countries.get(m.userId), m.expertise, m.availability, m.bio, active, m.maxMentees, active < m.maxMentees);
        });
    }

    @Transactional(readOnly = true)
    public Optional<MentorProfileRequest> myMentorProfile(Long userId) {
        return mentors.findById(userId).map(m -> new MentorProfileRequest(m.expertise, m.availability, m.bio, (int) m.maxMentees));
    }

    @Transactional
    public void saveMentorProfile(Long userId, MentorProfileRequest r) {
        var m = mentors.findById(userId).orElseGet(() -> { var n = new MentorProfile(); n.userId = userId; return n; });
        m.expertise = r.expertise().trim(); m.availability = r.availability(); m.bio = r.bio(); m.maxMentees = (short) (r.maxMentees() == null ? 10 : r.maxMentees());
        mentors.save(m);
    }

    // ----- requests -----
    @Transactional
    public void request(AuthUser me, MentorRequestInput r) {
        if (me.role() != Role.PARTICIPANT) throw ApiException.forbidden("Only participants can request a mentor.");
        if (!mentors.existsById(r.mentorId())) throw ApiException.notFound("Mentor not found");
        if (requests.existsByParticipantIdAndMentorIdAndStatus(me.id(), r.mentorId(), "PENDING")) throw ApiException.conflict("You already have a pending request with this mentor.");
        if (assignments.forMentee(me.id()).stream().anyMatch(a -> a.mentorId.equals(r.mentorId()))) throw ApiException.conflict("This mentor is already assigned to you.");
        var req = new MentorshipRequest();
        req.participantId = me.id(); req.mentorId = r.mentorId(); req.message = r.message();
        requests.save(req);
        notifications.notify(r.mentorId(), NotificationType.MENTOR_MESSAGE, "New mentorship request", directory.name(me.id()) + " would like you as a mentor.", "mentees");
    }

    @Transactional(readOnly = true)
    public List<RequestView> incoming(AuthUser me) { return toViews(requests.findByMentorIdAndStatusOrderByCreatedAtDesc(me.id(), "PENDING")); }
    @Transactional(readOnly = true)
    public List<RequestView> sent(AuthUser me) { return toViews(requests.findByParticipantIdOrderByCreatedAtDesc(me.id())); }

    @Transactional
    public void respond(AuthUser me, Long requestId, boolean accept) {
        var r = requests.findById(requestId).filter(x -> x.mentorId.equals(me.id()) && "PENDING".equals(x.status)).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (accept) {
            ensureCapacity(me.id());
            createAssignment(me.id(), r.participantId, null, me.id());
        }
        r.status = accept ? "ACCEPTED" : "DECLINED";
        notifications.notify(r.participantId, NotificationType.MENTOR_MESSAGE, accept ? "Mentor request accepted" : "Mentor request declined",
                directory.name(me.id()) + (accept ? " is now your mentor. Say hello in Messages!" : " can't take on a new mentee right now. You can request another mentor."), accept ? "messages" : "mentorship");
    }

    // ----- assignment (staff) -----
    @Transactional
    public void assign(AuthUser staff, AssignBody b, String ip) {
        if ((b.participantId() == null) == (b.teamId() == null)) throw ApiException.badRequest("Choose either a participant or a team.");
        var mentor = directory.user(b.mentorId()).filter(u -> u.getRole() == Role.MENTOR && u.getStatus() == UserStatus.ACTIVE).orElseThrow(() -> ApiException.badRequest("That user is not an active mentor."));
        ensureCapacity(mentor.getId());
        if (b.participantId() != null) directory.user(b.participantId()).filter(u -> u.getRole() == Role.PARTICIPANT).orElseThrow(() -> ApiException.badRequest("Participant not found"));
        if (b.teamId() != null && jdbc.queryForList("select 1 from teams where id = ?", Integer.class, b.teamId()).isEmpty()) throw ApiException.badRequest("Team not found");
        createAssignment(mentor.getId(), b.participantId(), b.teamId(), staff.id());
        audit.record(null, staff.email(), "MENTOR_ASSIGNED", "MentorshipAssignment", null, Map.of("mentorId", b.mentorId(), "participantId", String.valueOf(b.participantId()), "teamId", String.valueOf(b.teamId())), ip);
        notifications.notify(mentor.getId(), NotificationType.MENTOR_MESSAGE, "New mentee assigned", "You have been assigned a new " + (b.teamId() != null ? "team" : "participant") + " to mentor.", "mentees");
    }

    @Transactional
    public void end(AuthUser staff, Long id, String ip) {
        var a = assignments.findById(id).orElseThrow(() -> ApiException.notFound("Assignment not found"));
        a.status = "ENDED";
        audit.record(null, staff.email(), "MENTOR_UNASSIGNED", "MentorshipAssignment", String.valueOf(id), null, ip);
    }

    // ----- views -----
    /** Mentors see their mentees; participants see their mentors. */
    @Transactional(readOnly = true)
    public List<AssignmentView> mine(AuthUser me) {
        var list = me.role() == Role.MENTOR ? assignments.findByMentorIdAndStatus(me.id(), "ACTIVE") : assignments.forMentee(me.id());
        return list.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AssignmentView> all(int page, int size) { return PageResponse.of(assignments.findByStatus("ACTIVE", PageResponse.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::view); }

    private AssignmentView view(MentorshipAssignment a) {
        String mentee; String country = null; Integer learning = null, project = null;
        if (a.participantId != null) {
            mentee = directory.name(a.participantId);
            country = directory.countries(List.of(a.participantId)).get(a.participantId);
            learning = jdbc.queryForObject("select coalesce(round(avg(progress_pct)), 0) from enrollments where user_id = ? and status <> 'CANCELLED'", Integer.class, a.participantId);
        } else {
            mentee = jdbc.queryForList("select name from teams where id = ?", String.class, a.teamId).stream().findFirst().orElse("Team");
            project = jdbc.queryForList("select progress_pct from impact_projects where team_id = ?", Integer.class, a.teamId).stream().findFirst().orElse(null);
        }
        return new AssignmentView(a.id, a.participantId != null ? "PARTICIPANT" : "TEAM", a.mentorId, directory.name(a.mentorId), a.participantId, a.teamId, mentee, country, a.status, learning, project, a.createdAt);
    }

    private List<RequestView> toViews(List<MentorshipRequest> list) {
        var ids = new ArrayList<Long>();
        list.forEach(r -> { ids.add(r.participantId); ids.add(r.mentorId); });
        var names = directory.names(ids);
        var countries = directory.countries(list.stream().map(r -> r.participantId).toList());
        return list.stream().map(r -> new RequestView(r.id, r.participantId, names.get(r.participantId), countries.get(r.participantId), r.mentorId, names.get(r.mentorId), r.message, r.status, r.createdAt)).toList();
    }

    private void ensureCapacity(Long mentorId) {
        var m = mentors.findById(mentorId).orElseThrow(() -> ApiException.badRequest("Complete your mentor profile first."));
        if (assignments.countByMentorIdAndStatus(mentorId, "ACTIVE") >= m.maxMentees) throw ApiException.conflict("This mentor has reached their mentee limit.");
    }

    private void createAssignment(Long mentorId, Long participantId, Long teamId, Long by) {
        var a = new MentorshipAssignment();
        a.mentorId = mentorId; a.participantId = participantId; a.teamId = teamId; a.assignedBy = by;
        assignments.save(a);
        if (participantId != null) {
            conversations.ensureDirect(mentorId, participantId);
            notifications.notify(participantId, NotificationType.MENTOR_MESSAGE, "You have a mentor", directory.name(mentorId) + " will mentor you.", "mentorship");
        }
    }
}
