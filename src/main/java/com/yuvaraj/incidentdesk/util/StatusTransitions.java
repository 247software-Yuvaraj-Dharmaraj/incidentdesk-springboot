package com.yuvaraj.incidentdesk.util;

import com.yuvaraj.incidentdesk.domain.Status;

import java.util.List;
import java.util.Map;

/**
 * Allowed status transitions. CLOSED is terminal — a closed incident cannot be reopened.
 * OPEN/IN_PROGRESS/RESOLVED can move forward or step back to keep an active incident workable.
 */
public final class StatusTransitions {

    private static final Map<Status, List<Status>> ALLOWED = Map.of(
            Status.OPEN, List.of(Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED),
            Status.IN_PROGRESS, List.of(Status.OPEN, Status.RESOLVED, Status.CLOSED),
            Status.RESOLVED, List.of(Status.IN_PROGRESS, Status.CLOSED),
            Status.CLOSED, List.of());

    private StatusTransitions() {
    }

    public static boolean canTransition(Status from, Status to) {
        return from == to || ALLOWED.getOrDefault(from, List.of()).contains(to);
    }

    public static List<Status> nextStatuses(Status from) {
        return ALLOWED.getOrDefault(from, List.of());
    }
}
