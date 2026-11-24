package com.yep.platform.enrollment;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "enrollments")
public class Enrollment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "user_id") public Long userId;
    @Column(name = "activity_id") public Long activityId;
    @Enumerated(EnumType.STRING) public EnrollmentStatus status = EnrollmentStatus.ENROLLED;
    @Column(name = "progress_pct") public short progressPct;
    @Column(name = "completed_lessons") public int completedLessons;
    @Column(name = "final_score") public BigDecimal finalScore;
    @Column(name = "enrolled_at") public Instant enrolledAt = Instant.now();
    @Column(name = "completed_at") public Instant completedAt;
    public Enrollment() {}
}
