package com.yep.platform.achievement;

import jakarta.persistence.*;

@Entity
@Table(name = "achievements")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String code, name, description, icon, criteria;
    public boolean active = true;

    public Achievement() {
    }
}
