package com.yep.platform.mentorship;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "mentor_profiles")
public class MentorProfile extends BaseEntity {
    @Id @Column(name = "user_id") public Long userId;
    public String expertise, availability, bio;
    @Column(name = "max_mentees") public short maxMentees = 10;
    public MentorProfile() {}
}
