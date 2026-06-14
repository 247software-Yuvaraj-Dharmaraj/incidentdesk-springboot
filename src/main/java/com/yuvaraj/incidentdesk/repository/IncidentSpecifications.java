package com.yuvaraj.incidentdesk.repository;

import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.Status;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    /** Translates the resolved filter criteria into a JPA predicate set. */
    public static Specification<Incident> matching(IncidentFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.type() != null) {
                predicates.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), f.priority()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + f.q().toLowerCase() + "%"));
            }
            if (f.assigneeId() != null) {
                if ("unassigned".equals(f.assigneeId())) {
                    predicates.add(cb.isNull(root.get("assignee")));
                } else {
                    predicates.add(cb.equal(root.get("assignee").get("id"), f.assigneeId()));
                }
            }
            if (Boolean.TRUE.equals(f.overdue())) {
                predicates.add(cb.lessThan(root.<Instant>get("dueDate"), Instant.now()));
                predicates.add(cb.not(root.get("status").in(Status.RESOLVED, Status.CLOSED)));
            }
            if (f.reporterId() != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), f.reporterId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Keyset boundary for cursor pagination (ordered by createdAt desc). */
    public static Specification<Incident> createdBefore(Instant boundary) {
        return (root, query, cb) -> cb.lessThan(root.<Instant>get("createdAt"), boundary);
    }

    /** Eager-fetches reporter/assignee for the data query, but not for the count query. */
    public static Specification<Incident> withRelations() {
        return (root, query, cb) -> {
            Class<?> resultType = query == null ? null : query.getResultType();
            if (resultType != null && resultType != Long.class && resultType != long.class) {
                root.fetch("reporter", JoinType.INNER);
                root.fetch("assignee", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }
}
