package com.yep.platform.assessment;

import jakarta.persistence.*;

@Entity @Table(name = "attempt_answers")
public class AttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "attempt_id") public Long attemptId;
    @Column(name = "question_id") public Long questionId;
    @Column(name = "option_id") public Long optionId;
    public boolean correct;
    public AttemptAnswer() {}
}
