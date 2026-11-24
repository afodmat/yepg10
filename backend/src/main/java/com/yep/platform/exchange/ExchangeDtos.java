package com.yep.platform.exchange;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ExchangeDtos {
    private ExchangeDtos() {}

    public record SessionView(Long id, String title, String description, String topic, Instant startsAt, int durationMinutes, Long facilitatorId, String facilitatorName,
                              String status, String phase, long participantCount, List<String> countries, boolean registered, String meetingLink,
                              List<String> discussionPrompts, List<String> resources) {}

    public record SessionRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 4000) String description, @NotBlank @Size(max = 100) String topic,
                                 @NotNull Instant startsAt, @NotNull @Min(15) @Max(480) Integer durationMinutes,
                                 @Pattern(regexp = "^$|^https://\\S{3,280}$", message = "Meeting link must start with https://") String meetingLink,
                                 @Size(max = 3000) String discussionPrompts, @Size(max = 3000) String resources, ExchangeStatus status) {}
}
