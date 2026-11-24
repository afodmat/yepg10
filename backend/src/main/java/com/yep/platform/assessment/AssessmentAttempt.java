package com.yep.platform.assessment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "assessment_attempts")
public class AssessmentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "assessment_id") public Long assessmentId;
    @Column(name = "user_id") public Long userId;
    @Column(name = "score_pct") public BigDecimal scorePct;
    public boolean passed;
    @Column(name = "submitted_at") public Instant submittedAt = Instant.now();
    public AssessmentAttempt() {}
}
