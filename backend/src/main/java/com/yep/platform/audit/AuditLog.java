package com.yep.platform.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_id") private Long actorId;
    @Column(name = "actor_email") private String actorEmail;
    @Column(nullable = false) private String action;
    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id") private String entityId;
    @JdbcTypeCode(SqlTypes.JSON) private Map<String, Object> metadata;
    @Column(name = "ip_address") private String ipAddress;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected AuditLog() {}
    public AuditLog(Long actorId, String actorEmail, String action, String entityType, String entityId, Map<String, Object> metadata, String ip) {
        this.actorId = actorId; this.actorEmail = actorEmail; this.action = action; this.entityType = entityType;
        this.entityId = entityId; this.metadata = metadata; this.ipAddress = ip;
    }
    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
