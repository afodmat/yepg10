package com.yep.platform.notification;

import com.yep.platform.common.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public record NotificationView(Long id, NotificationType type, String title, String body, String link, boolean read, Instant createdAt) {}
    public record PreferenceView(String type, boolean enabled) {}

    private final NotificationRepository repo;
    private final NotificationPreferenceRepository prefs;
    public NotificationService(NotificationRepository repo, NotificationPreferenceRepository prefs) { this.repo = repo; this.prefs = prefs; }

    /** Persists a notification unless the user switched that type off. SYSTEM notifications are always delivered. */
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String body, String link) {
        if (type != NotificationType.SYSTEM) {
            var p = prefs.findById(keyOf(userId, type.name()));
            if (p.isPresent() && !p.get().enabled) return;
        }
        var n = new Notification();
        n.userId = userId; n.type = type; n.title = title; n.body = body; n.link = link;
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> list(Long userId, int page, int size) {
        return PageResponse.of(repo.findByUserIdOrderByCreatedAtDesc(userId, PageResponse.request(page, size, Sort.unsorted())), n -> new NotificationView(n.id, n.type, n.title, n.body, n.link, n.readAt != null, n.createdAt));
    }
    @Transactional(readOnly = true) public long unread(Long userId) { return repo.countByUserIdAndReadAtIsNull(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        var n = repo.findById(id).filter(x -> x.userId.equals(userId)).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (n.readAt == null) n.readAt = Instant.now();
    }
    @Transactional public void markAllRead(Long userId) { repo.markAllRead(userId, Instant.now()); }

    @Transactional(readOnly = true)
    public List<PreferenceView> preferences(Long userId) {
        Map<String, Boolean> saved = new HashMap<>();
        prefs.findByUserId(userId).forEach(p -> saved.put(p.type, p.enabled));
        return Arrays.stream(NotificationType.values()).filter(t -> t != NotificationType.SYSTEM).map(t -> new PreferenceView(t.name(), saved.getOrDefault(t.name(), true))).toList();
    }
    @Transactional
    public void setPreference(Long userId, String type, boolean enabled) {
        NotificationType t;
        try { t = NotificationType.valueOf(type); } catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown notification type"); }
        if (t == NotificationType.SYSTEM) throw ApiException.badRequest("System notifications cannot be switched off");
        prefs.save(new NotificationPreference(userId, t.name(), enabled));
    }

    private static NotificationPreference.Key keyOf(Long u, String t) { var k = new NotificationPreference.Key(); k.userId = u; k.type = t; return k; }
}
