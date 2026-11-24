package com.yep.platform.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AssessmentDtos {
    private AssessmentDtos() {}

    public record OptionInput(@NotBlank @Size(max = 300) String text, boolean correct) {}
    public record QuestionInput(@NotBlank @Size(max = 500) String text, @Min(1) @Max(20) Integer points,
                                @NotNull @Size(min = 2, max = 6) List<@Valid OptionInput> options) {}
    public record AssessmentRequest(@NotBlank @Size(max = 160) String title, @NotNull @Min(1) @Max(100) Integer passMark,
                                    @NotNull @Min(1) @Max(20) Integer maxAttempts,
                                    @NotNull @Size(min = 1, max = 50) List<@Valid QuestionInput> questions) {}

    /** `correct` is null (and omitted from JSON) for participants. */
    public record OptionView(Long id, String text, Boolean correct) {}
    public record QuestionView(Long id, String text, int points, List<OptionView> options) {}
    public record AssessmentView(Long id, Long activityId, String title, int passMark, int maxAttempts, int attemptsUsed, boolean passed, List<QuestionView> questions) {}

    public record SubmitRequest(@NotNull Map<Long, Long> answers) {}
    public record AttemptResult(Long attemptId, BigDecimal scorePct, boolean passed, int correct, int total, int attemptsLeft, String feedback) {}
    public record AttemptRow(Long id, Long userId, String participant, BigDecimal scorePct, boolean passed, Instant submittedAt) {}
}
