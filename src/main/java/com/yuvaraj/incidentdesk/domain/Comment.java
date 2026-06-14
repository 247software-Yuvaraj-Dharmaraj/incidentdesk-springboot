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
@Table(name = "comments")
public class Comment {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean internal = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(Incident incident, User author, String body, boolean internal) {
        this.incident = incident;
        this.author = author;
        this.body = body;
        this.internal = internal;
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

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternal() {
        return internal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
