package com.yep.platform.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "user_id") public Long userId;
    @Enumerated(EnumType.STRING) public NotificationType type;
    public String title, body, link;
    @Column(name = "read_at") public Instant readAt;
    @Column(name = "created_at") public Instant createdAt = Instant.now();
    public Notification() {}
}
