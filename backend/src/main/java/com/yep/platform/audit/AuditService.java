package com.yep.platform.audit;

import com.yep.platform.user.User;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records security/admin events. Never put passwords, tokens or other secrets in metadata. */
@Service
public class AuditService {
    private final AuditLogRepository repo;
    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    /** REQUIRES_NEW so a failed login is still recorded when the caller's transaction rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, String actorEmail, String action, String entityType, String entityId, Map<String, Object> meta, String ip) {
        repo.save(new AuditLog(actor == null ? null : actor.getId(), actorEmail, action, entityType, entityId, meta, ip));
    }
}
