package com.yep.platform.assessment;

import jakarta.persistence.*;

@Entity @Table(name = "question_options")
public class QuestionOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "question_id") public Long questionId;
    public String text;
    public boolean correct;
    public short position;
    public QuestionOption() {}
}
