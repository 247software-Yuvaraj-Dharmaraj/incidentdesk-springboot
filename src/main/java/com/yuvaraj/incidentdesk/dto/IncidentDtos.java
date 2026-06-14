package com.yuvaraj.incidentdesk.dto;

import com.yuvaraj.incidentdesk.domain.AuditLog;
import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class IncidentDtos {

    private IncidentDtos() {
    }

    public record CreateIncidentRequest(
            @NotBlank @Size(min = 3, max = 140, message = "Title must be between 3 and 140 characters") String title,
            IncidentType type,
            Priority priority,
            @Size(max = 2000) String description) {
    }

    public record UpdateIncidentRequest(
            Status status,
            Priority priority,
            JsonNullable<String> assigneeId,
            JsonNullable<String> dueDate,
            String expectedUpdatedAt) {
    }

    public record BulkUpdateRequest(
            @NotEmpty @Size(max = 100) List<String> ids,
            Status status,
            Priority priority,
            JsonNullable<String> assigneeId) {
    }

    public record BulkDeleteRequest(
            @NotEmpty @Size(max = 100) List<String> ids) {
    }

    public record TriageRequest(
            @NotBlank @Size(min = 3, max = 140) String title,
            @Size(max = 2000) String description) {
    }

    public record TriageResponse(IncidentType type, Priority priority, String summary) {
    }

    public record TriageStatus(boolean enabled) {
    }

    public record AuditLogResponse(
            String id,
            String incidentId,
            String actorId,
            String field,
            String oldValue,
            String newValue,
            Instant createdAt,
            AuthDtos.UserPreview actor) {

        public static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(
                    a.getId(),
                    a.getIncident().getId(),
                    a.getActor().getId(),
                    a.getField(),
                    a.getOldValue(),
                    a.getNewValue(),
                    a.getCreatedAt(),
                    AuthDtos.UserPreview.from(a.getActor()));
        }
    }

    public record IncidentResponse(
            String id,
            String title,
            IncidentType type,
            Priority priority,
            Status status,
            String description,
            Instant dueDate,
            Instant resolvedAt,
            String reporterId,
            String assigneeId,
            Instant createdAt,
            Instant updatedAt,
            AuthDtos.UserPreview reporter,
            AuthDtos.UserPreview assignee,
            List<AuditLogResponse> auditLogs) {

        // List rows omit description (a TEXT field up to 2000 chars) — the detail
        // view refetches the full incident by id. With Jackson non_null inclusion,
        // the null description is dropped from the list JSON entirely.
        public static IncidentResponse summary(Incident i) {
            return build(i, null, null);
        }

        public static IncidentResponse detail(Incident i, List<AuditLog> audit) {
            return build(i, i.getDescription(), audit.stream().map(AuditLogResponse::from).toList());
        }

        private static IncidentResponse build(Incident i, String description, List<AuditLogResponse> audit) {
            return new IncidentResponse(
                    i.getId(),
                    i.getTitle(),
                    i.getType(),
                    i.getPriority(),
                    i.getStatus(),
                    description,
                    i.getDueDate(),
                    i.getResolvedAt(),
                    i.getReporter().getId(),
                    i.getAssignee() == null ? null : i.getAssignee().getId(),
                    i.getCreatedAt(),
                    i.getUpdatedAt(),
                    AuthDtos.UserPreview.from(i.getReporter()),
                    AuthDtos.UserPreview.from(i.getAssignee()),
                    audit);
        }
    }

    public record ListResult(List<IncidentResponse> items, String nextCursor, long total) {
    }

    public record Stats(long total, Map<String, Long> byStatus) {
    }

    public record TrendPoint(String date, long created, long resolved) {
    }

    public record Metrics(Double mttrHours, long resolvedCount, List<TrendPoint> trend) {
    }

    public record BulkUpdateResult(long updated, long skipped) {
    }

    public record BulkDeleteResult(long deleted) {
    }
}
