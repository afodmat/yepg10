package com.yep.platform.assessment;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "assessments")
public class Assessment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "activity_id") public Long activityId;
    public String title;
    @Column(name = "pass_mark") public short passMark = 60;
    @Column(name = "max_attempts") public short maxAttempts = 3;
    public Assessment() {}
}
