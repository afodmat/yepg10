package com.yep.platform.activity;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class ActivityDtos {
    private ActivityDtos() {}

    public record ActivityView(Long id, String title, String description, String category, String level, String objectives,
                               Long facilitatorId, String facilitatorName, LocalDate startDate, LocalDate endDate, Integer durationHours,
                               int capacity, long enrolledCount, int lessonsTotal, ActivityStatus status, String thumbnailUrl) {}

    public record ActivityRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 60) String category,
            @NotBlank @Size(max = 30) String level,
            @Size(max = 4000) String objectives,
            LocalDate startDate, LocalDate endDate,
            @Min(1) @Max(1000) Integer durationHours,
            @NotNull @Min(1) @Max(100000) Integer capacity,
            @NotNull @Min(1) @Max(100) Integer lessonsTotal,
            @NotNull ActivityStatus status,
            @Size(max = 300) String thumbnailUrl) {}
}
