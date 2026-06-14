package com.yuvaraj.incidentdesk.repository;

import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Status;

/**
 * Resolved filter criteria for an incident list query.
 * {@code reporterId} is set by the service for REPORTER users (own incidents only) and null for admins.
 */
public record IncidentFilter(
        Status status,
        IncidentType type,
        Priority priority,
        String q,
        String assigneeId,
        Boolean overdue,
        String reporterId) {
}
