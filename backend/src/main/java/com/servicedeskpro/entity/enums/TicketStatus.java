package com.servicedeskpro.entity.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REOPENED;

    /**
     * Validates if a state transition from 'this' status to 'target' status is permitted.
     * Enforces enterprise ITIL lifecycle rules.
     */
    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case OPEN -> Set.of(ASSIGNED, IN_PROGRESS, CLOSED).contains(target);
            case ASSIGNED -> Set.of(IN_PROGRESS, OPEN, CLOSED).contains(target);
            case IN_PROGRESS -> Set.of(RESOLVED, ASSIGNED, CLOSED).contains(target);
            case RESOLVED -> Set.of(CLOSED, REOPENED).contains(target);
            case REOPENED -> Set.of(ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED).contains(target);
            case CLOSED -> false; // Closed tickets are immutable; cannot transition
        };
    }
}