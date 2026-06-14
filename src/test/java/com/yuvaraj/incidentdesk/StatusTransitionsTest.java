package com.yuvaraj.incidentdesk;

import com.yuvaraj.incidentdesk.domain.Status;
import com.yuvaraj.incidentdesk.util.StatusTransitions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTransitionsTest {

    @Test
    void closedIsTerminal() {
        assertFalse(StatusTransitions.canTransition(Status.CLOSED, Status.OPEN));
        assertFalse(StatusTransitions.canTransition(Status.CLOSED, Status.IN_PROGRESS));
        assertTrue(StatusTransitions.canTransition(Status.CLOSED, Status.CLOSED));
    }

    @Test
    void openCanMoveForwardAndToClosed() {
        assertTrue(StatusTransitions.canTransition(Status.OPEN, Status.IN_PROGRESS));
        assertTrue(StatusTransitions.canTransition(Status.OPEN, Status.RESOLVED));
        assertTrue(StatusTransitions.canTransition(Status.OPEN, Status.CLOSED));
    }

    @Test
    void resolvedCanReopenToInProgressbutNotOpen() {
        assertTrue(StatusTransitions.canTransition(Status.RESOLVED, Status.IN_PROGRESS));
        assertFalse(StatusTransitions.canTransition(Status.RESOLVED, Status.OPEN));
    }
}
