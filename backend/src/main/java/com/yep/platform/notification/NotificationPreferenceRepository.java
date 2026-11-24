package com.yep.platform.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, NotificationPreference.Key> {
    List<NotificationPreference> findByUserId(Long userId);
}
