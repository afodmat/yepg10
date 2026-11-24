package com.yep.platform.assessment;

import jakarta.persistence.*;

@Entity @Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "assessment_id") public Long assessmentId;
    public String text;
    public short points = 1;
    public short position;
    public Question() {}
}
