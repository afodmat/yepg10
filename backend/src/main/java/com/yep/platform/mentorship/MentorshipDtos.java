package com.yep.platform.mentorship;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class MentorshipDtos {
    private MentorshipDtos() {}
    public record MentorView(Long userId, String name, String country, String expertise, String availability, String bio, long activeMentees, int maxMentees, boolean accepting) {}
    public record MentorProfileRequest(@NotBlank @Size(max = 300) String expertise, @Size(max = 200) String availability, @Size(max = 1000) String bio, @Min(1) @Max(100) Integer maxMentees) {}
    public record MentorRequestInput(@NotNull Long mentorId, @Size(max = 1000) String message) {}
    public record RequestView(Long id, Long participantId, String participant, String participantCountry, Long mentorId, String mentor, String message, String status, Instant createdAt) {}
    public record RespondBody(boolean accept) {}
    public record AssignBody(@NotNull Long mentorId, Long participantId, Long teamId) {}
    public record AssignmentView(Long id, String type, Long mentorId, String mentor, Long participantId, Long teamId, String mentee, String country, String status, Integer learningProgress, Integer projectProgress, Instant createdAt) {}
}
