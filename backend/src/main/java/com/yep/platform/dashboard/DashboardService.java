package com.yep.platform.dashboard;

import com.yep.platform.achievement.AchievementService;
import com.yep.platform.achievement.AchievementService.AchievementView;
import com.yep.platform.certificate.CertificateService;
import com.yep.platform.enrollment.EnrollmentService;
import com.yep.platform.enrollment.EnrollmentService.EnrollmentView;
import com.yep.platform.exchange.ExchangeDtos.SessionView;
import com.yep.platform.exchange.ExchangeService;
import com.yep.platform.mentorship.MentorshipDtos.AssignmentView;
import com.yep.platform.mentorship.MentorshipService;
import com.yep.platform.messaging.ConversationService;
import com.yep.platform.messaging.ConversationService.ConversationView;
import com.yep.platform.notification.NotificationService;
import com.yep.platform.security.AuthUser;
import com.yep.platform.team.TeamDtos.TeamView;
import com.yep.platform.team.TeamService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One call, one round-trip: the participant dashboard is assembled server-side for low-bandwidth clients. */
@Service
public class DashboardService {
    public record DashboardView(List<EnrollmentView> learning, List<SessionView> upcomingSessions, TeamView team, List<ConversationView> recentMessages, long unreadMessages,
                                long unreadNotifications, List<AchievementView> achievements, int achievementsEarned, int achievementsTotal, int certificates,
                                List<AssignmentView> mentors, int pendingInvitations) {}

    private final EnrollmentService enrollments; private final ExchangeService exchange; private final TeamService teams; private final ConversationService conversations;
    private final NotificationService notifications; private final AchievementService achievements; private final CertificateService certificates; private final MentorshipService mentorship;

    public DashboardService(EnrollmentService enrollments, ExchangeService exchange, TeamService teams, ConversationService conversations, NotificationService notifications,
                            AchievementService achievements, CertificateService certificates, MentorshipService mentorship) {
        this.enrollments = enrollments; this.exchange = exchange; this.teams = teams; this.conversations = conversations;
        this.notifications = notifications; this.achievements = achievements; this.certificates = certificates; this.mentorship = mentorship;
    }

    @Transactional(readOnly = true)
    public DashboardView build(AuthUser me) {
        var convs = conversations.list(me);
        var ach = achievements.forUser(me.id());
        return new DashboardView(enrollments.mine(me.id()), exchange.upcomingFor(me.id(), me), teams.myTeam(me).orElse(null),
                convs.stream().limit(3).toList(), convs.stream().mapToLong(ConversationView::unread).sum(), notifications.unread(me.id()),
                ach.stream().filter(AchievementView::earned).limit(4).toList(), (int) ach.stream().filter(AchievementView::earned).count(), ach.size(),
                certificates.mine(me.id()).size(), mentorship.mine(me), teams.myInvitations(me).size());
    }
}
