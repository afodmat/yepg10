package com.yep.platform.team;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class TeamDtos {
    private TeamDtos() {}

    public record MemberView(Long userId, String name, String country, String role, String title, Instant joinedAt) {}
    public record ProjectSummary(Long id, String name, int progressPct, String status) {}
    public record TeamView(Long id, String name, String description, Long leaderId, String leaderName, String country, String institution, String category,
                           boolean open, String status, long memberCount, boolean member, Instant createdAt,
                           List<MemberView> members, ProjectSummary project, List<String> mentors) {}
    public record TeamRequest(@NotBlank @Size(min = 3, max = 80) String name, @Size(max = 1000) String description, @Size(max = 60) String country,
                              @Size(max = 160) String institution, @Size(max = 60) String category, Boolean open) {}
    public record InviteRequest(@NotBlank @Email String email) {}
    public record TitleRequest(@Size(max = 60) String title) {}
    public record InvitationView(Long id, Long teamId, String teamName, String invitedBy, Instant createdAt) {}
}
