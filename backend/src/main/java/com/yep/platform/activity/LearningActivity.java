package com.yep.platform.activity;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "learning_activities")
public class LearningActivity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String title, description, category, level, objectives;
    @Column(name = "facilitator_id") public Long facilitatorId;
    @Column(name = "start_date") public LocalDate startDate;
    @Column(name = "end_date") public LocalDate endDate;
    @Column(name = "duration_hours") public Integer durationHours;
    public int capacity = 200;
    @Column(name = "lessons_total") public int lessonsTotal = 4;
    @Enumerated(EnumType.STRING) public ActivityStatus status = ActivityStatus.DRAFT;
    @Column(name = "thumbnail_url") public String thumbnailUrl;
    public LearningActivity() {}
}
