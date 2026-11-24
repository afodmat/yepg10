package com.yep.platform.config;

import com.yep.platform.activity.*;
import com.yep.platform.activity.ActivityDtos.ActivityRequest;
import com.yep.platform.assessment.AssessmentDtos.*;
import com.yep.platform.assessment.AssessmentService;
import com.yep.platform.enrollment.EnrollmentService;
import com.yep.platform.exchange.*;
import com.yep.platform.exchange.ExchangeDtos.SessionRequest;
import com.yep.platform.mentorship.*;
import com.yep.platform.mentorship.MentorshipDtos.*;
import com.yep.platform.messaging.ConversationService;
import com.yep.platform.project.ProjectService;
import com.yep.platform.project.ProjectDtos.*;
import com.yep.platform.security.AuthUser;
import com.yep.platform.team.TeamService;
import com.yep.platform.team.TeamDtos.TeamRequest;
import com.yep.platform.user.*;
import java.time.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev-only sample content so a fresh install can be demonstrated end to end. Goes through the real services
 * (so every business rule applies) and only runs on an empty database.
 */
@Component @Profile("dev") @Order(2)
public class DevContentSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevContentSeeder.class);
    private final UserRepository users;
    private final LearningActivityRepository activityRepo;
    private final ActivityService activities;
    private final AssessmentService assessments;
    private final EnrollmentService enrollments;
    private final ExchangeService exchange;
    private final TeamService teams;
    private final ProjectService projects;
    private final MentorshipService mentorship;
    private final ConversationService conversations;

    public DevContentSeeder(UserRepository users, LearningActivityRepository activityRepo, ActivityService activities, AssessmentService assessments, EnrollmentService enrollments,
                            ExchangeService exchange, TeamService teams, ProjectService projects, MentorshipService mentorship, ConversationService conversations) {
        this.users = users; this.activityRepo = activityRepo; this.activities = activities; this.assessments = assessments; this.enrollments = enrollments;
        this.exchange = exchange; this.teams = teams; this.projects = projects; this.mentorship = mentorship; this.conversations = conversations;
    }

    private AuthUser as(String email) { return users.findByEmailIgnoreCase(email).map(u -> new AuthUser(u.getId(), u.getEmail(), u.getRole())).orElse(null); }

    @Override
    public void run(String... args) {
        var admin = as("admin@yep.local"); var fac = as("facilitator@yep.local"); var mentor = as("mentor@yep.local");
        var amina = as("amina@yep.local"); var peter = as("peter@yep.local"); var brian = as("brian@yep.local"); var wanjiru = as("wanjiru@yep.local");
        if (admin == null || fac == null || mentor == null || amina == null || peter == null || brian == null || wanjiru == null) return;
        if (activityRepo.count() > 0) return;

        var today = LocalDate.now();
        String[][] acts = {
            {"Digital Literacy Foundations", "Digital Skills", "Level 1", "Build confidence using digital tools safely, effectively, and creatively."},
            {"AI Literacy for Young Leaders", "AI Literacy", "Level 1", "Understand AI, verify its outputs, and use it ethically in your work."},
            {"Intercultural Communication", "Intercultural Communication", "Level 1", "Connect across cultures and collaborate with peers across East Africa."},
            {"Designing Impact Projects", "Leadership", "Level 2", "Turn a community challenge into a practical, testable solution."},
            {"Media Literacy & Truth", "Media Literacy", "Level 1", "Recognise misinformation, deepfakes, and unreliable online sources."}};
        Long firstId = null;
        for (int i = 0; i < acts.length; i++) {
            var v = activities.create(new ActivityRequest(acts[i][0], acts[i][3], acts[i][1], acts[i][2], "Apply what you learn in a real situation\nShare your learning with your peers",
                    today.plusDays(7L * i), today.plusDays(7L * i + 28), 12 + 2 * i, 200, 4, ActivityStatus.OPEN, null), fac, "seed");
            if (i == 0) firstId = v.id();
        }

        assessments.save(firstId, new AssessmentRequest("Digital Literacy check", 60, 3, List.of(
            new QuestionInput("Which is the strongest password?", 1, List.of(new OptionInput("password123", false), new OptionInput("Kampala2020", false), new OptionInput("t7!Rq-lemon-Bridge-29", true))),
            new QuestionInput("What should you do with a suspicious link in a message?", 1, List.of(new OptionInput("Click it to see where it goes", false), new OptionInput("Check the sender and do not click", true), new OptionInput("Forward it to friends", false))),
            new QuestionInput("Two-factor authentication helps by…", 1, List.of(new OptionInput("Adding a second proof of identity", true), new OptionInput("Making your phone faster", false), new OptionInput("Hiding your email address", false))))), fac, "seed");

        exchange.create(new SessionRequest("Youth & Digital Skills: East Africa Dialogue", "A facilitated conversation between young people in Uganda, Kenya and Tanzania on the digital skills employers need.", "Digital skills and employability",
                Instant.now().plus(Duration.ofDays(5)), 90, "https://meet.example.org/yep-dialogue", "What digital skill changed your life most?\nWhat is hard about learning online where you live?", "Session reading pack: https://example.org/yep-reading", null), fac, "seed");
        exchange.create(new SessionRequest("Spotting Misinformation Together", "Practise verifying claims with peers from across the region.", "Media literacy",
                Instant.now().plus(Duration.ofDays(12)), 60, "https://meet.example.org/yep-misinfo", "Share a rumour you saw recently. How would you check it?", null, null), fac, "seed");
        exchange.create(new SessionRequest("Welcome to YEP Virtual Exchange", "Introductions and how Virtual Exchange works.", "Orientation",
                Instant.now().minus(Duration.ofDays(14)), 60, "https://meet.example.org/yep-welcome", null, null, null), fac, "seed");

        mentorship.saveMentorProfile(mentor.id(), new MentorProfileRequest("Youth employment, social enterprise, project design", "Weekday evenings (EAT)", "Social entrepreneur who has coached over 40 young founders across Uganda.", 8));
        mentorship.assign(admin, new AssignBody(mentor.id(), amina.id(), null), "seed");

        var bf = teams.create(new TeamRequest("Bright Futures", "Bringing practical digital skills to underserved communities.", "Uganda", "Makerere University", "Employment", true), amina, "seed");
        teams.join(bf.id(), peter);
        var gi = teams.create(new TeamRequest("Green Innovators", "Technology for smallholder farmers.", "Uganda", "Busitema University", "Agriculture", true), brian, "seed");
        var tu = teams.create(new TeamRequest("Team Ubuntu", "Student wellbeing across East African universities.", "Kenya", "University of Nairobi", "Health", true), wanjiru, "seed");

        project(amina, bf.id(), "Solar Skills Hub", "Employment", "Kampala, Uganda", "Mobile solar-powered learning hubs bringing practical digital skills to underserved communities.", 4);
        project(brian, gi.id(), "AgriLink Youth", "Agriculture", "Jinja, Uganda", "A lightweight marketplace connecting young farmers to buyers and trusted growing advice.", 5);
        project(wanjiru, tu.id(), "MindSpace EA", "Health", "Nairobi, Kenya", "Peer-led wellbeing circles and private referrals designed for university students.", 3);

        enrollments.enroll(amina.id(), firstId);
        enrollments.completeLesson(amina.id(), firstId);
        enrollments.completeLesson(amina.id(), firstId);
        var second = activityRepo.findAll().stream().filter(a -> a.title.startsWith("AI Literacy")).findFirst().orElseThrow();
        enrollments.enroll(amina.id(), second.id);
        enrollments.completeLesson(amina.id(), second.id);

        var chat = conversations.direct(amina, mentor.id());
        conversations.send(mentor, chat.id(), "Welcome to YEP, Amina! Send me your draft problem statement when it is ready and we will refine it together.");
        log.info("Seeded demo content (activities, sessions, teams, projects, mentorship, messages)");
    }

    private void project(AuthUser leader, Long teamId, String name, String category, String location, String description, int doneMilestones) {
        var created = projects.create(new ProjectRequest(teamId, name, description, "Many young people lack access to practical digital skills and tools.",
                "A practical, low-cost approach built with the community.", category, location, LocalDate.now().minusDays(30), LocalDate.now().plusDays(90)), leader, "seed");
        for (int i = 0; i < doneMilestones; i++) projects.patchMilestone(created.id(), created.milestones().get(i).id(), "DONE", leader);
        projects.postUpdate(created.id(), new UpdateRequest("Completed our community research interviews and drafted the plan.", null, null), leader);
    }
}
