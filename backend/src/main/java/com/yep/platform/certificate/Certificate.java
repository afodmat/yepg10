package com.yep.platform.certificate;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "certificates")
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "user_id") public Long userId;
    @Column(name = "activity_id") public Long activityId;
    @Column(name = "certificate_number") public String certificateNumber;
    @Column(name = "verification_code") public String verificationCode;
    public String issuer = "Youth Empowerment Project";
    public String status = "VALID";
    @Column(name = "issued_at") public Instant issuedAt = Instant.now();
    public Certificate() {}
}
