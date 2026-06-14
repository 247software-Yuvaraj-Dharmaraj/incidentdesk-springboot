package com.yuvaraj.incidentdesk.domain;

import com.yuvaraj.incidentdesk.util.Cuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false, length = 50)
    private String field;

    @Column(columnDefinition = "text")
    private String oldValue;

    @Column(columnDefinition = "text")
    private String newValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(Incident incident, User actor, String field, String oldValue, String newValue) {
        this.incident = incident;
        this.actor = actor;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = Cuid.generate();
        }
    }

    public String getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public User getActor() {
        return actor;
    }

    public String getField() {
        return field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
