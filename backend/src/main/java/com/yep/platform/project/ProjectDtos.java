package com.yep.platform.project;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record MilestoneView(Long id, String name, String status, Instant completedAt) {}
    public record PostView(Long id, Long authorId, String author, String content, Instant createdAt) {}
    public record AttachmentView(Long fileId, String name, String contentType) {}
    public record ProjectView(Long id, Long teamId, String teamName, String name, String description, String problemStatement, String solution, String category, String location,
                              Long leaderId, String leaderName, String status, int progressPct, LocalDate startDate, LocalDate targetDate, List<String> mentors,
                              boolean canView, boolean canEdit, boolean canContribute, boolean canReview,
                              List<MilestoneView> milestones, List<PostView> updates, List<PostView> comments, List<AttachmentView> attachments) {}

    public record ProjectRequest(Long teamId,
                                 @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 4000) String description,
                                 @Size(max = 4000) String problemStatement, @Size(max = 4000) String solution,
                                 @NotBlank @Pattern(regexp = "Employment|Education|Agriculture|Health|Climate Change|Digital Inclusion", message = "Choose a valid category") String category,
                                 @Size(max = 120) String location, LocalDate startDate, LocalDate targetDate) {}
    public record UpdateRequest(@NotBlank @Size(max = 2000) String content, Long milestoneId, @Pattern(regexp = "PENDING|IN_PROGRESS|DONE") String milestoneStatus) {}
    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {}
    public record MilestoneRequest(@NotBlank @Pattern(regexp = "PENDING|IN_PROGRESS|DONE") String status) {}
    public record StatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|SUBMITTED|COMPLETED") String status) {}
    public record AttachRequest(@NotNull Long fileId) {}
}
