package com.yep.platform.project;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "project_milestones")
public class ProjectMilestone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "project_id") public Long projectId;
    public String name;
    public short position;
    public String status = "PENDING";
    @Column(name = "completed_at") public Instant completedAt;
    public ProjectMilestone() {}
}
