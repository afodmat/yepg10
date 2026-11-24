package com.yep.platform.notification;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity @Table(name = "notification_preferences") @IdClass(NotificationPreference.Key.class)
public class NotificationPreference {
    public static class Key implements Serializable {
        public Long userId; public String type;
        public Key() {}
        @Override public boolean equals(Object o) { return o instanceof Key k && java.util.Objects.equals(userId, k.userId) && java.util.Objects.equals(type, k.type); }
        @Override public int hashCode() { return java.util.Objects.hash(userId, type); }
    }
    @Id @Column(name = "user_id") public Long userId;
    @Id public String type;
    public boolean enabled = true;
    public NotificationPreference() {}
    public NotificationPreference(Long userId, String type, boolean enabled) { this.userId = userId; this.type = type; this.enabled = enabled; }
}
