package com.yep.platform.project;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "impact_projects")
public class ImpactProject extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "team_id") public Long teamId;
    public String name, description, category, location, status = "ACTIVE";
    @Column(name = "problem_statement") public String problemStatement;
    public String solution;
    @Column(name = "leader_id") public Long leaderId;
    @Column(name = "progress_pct") public short progressPct;
    @Column(name = "start_date") public LocalDate startDate;
    @Column(name = "target_date") public LocalDate targetDate;
    public ImpactProject() {}
}
