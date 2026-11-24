package com.yep.platform.assessment;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public final class AssessmentRepositories {
    private AssessmentRepositories() {}
    public interface AssessmentRepository extends JpaRepository<Assessment, Long> { Optional<Assessment> findByActivityId(Long activityId); }
    public interface QuestionRepository extends JpaRepository<Question, Long> { List<Question> findByAssessmentIdOrderByPosition(Long assessmentId); }
    public interface OptionRepository extends JpaRepository<QuestionOption, Long> { List<QuestionOption> findByQuestionIdInOrderByPosition(Collection<Long> questionIds); }
    public interface AttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
        List<AssessmentAttempt> findByAssessmentIdAndUserIdOrderBySubmittedAtDesc(Long assessmentId, Long userId);
        long countByAssessmentIdAndUserId(Long assessmentId, Long userId);
        long countByAssessmentId(Long assessmentId);
        Page<AssessmentAttempt> findByAssessmentId(Long assessmentId, Pageable p);
    }
    public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {}
}
