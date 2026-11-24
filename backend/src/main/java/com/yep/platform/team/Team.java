package com.yep.platform.team;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "teams")
public class Team extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name, description, country, institution, category;
    @Column(name = "leader_id") public Long leaderId;
    public boolean open = true;
    public String status = "ACTIVE";
    public Team() {}
}
