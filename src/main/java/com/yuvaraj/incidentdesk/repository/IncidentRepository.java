package com.yuvaraj.incidentdesk.repository;

import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, String>, JpaSpecificationExecutor<Incident> {

    @Query("""
            select i.status as status, count(i) as count
            from Incident i
            where (:reporterId is null or i.reporter.id = :reporterId)
            group by i.status
            """)
    List<StatusCount> countByStatus(@Param("reporterId") String reporterId);

    @Query("""
            select i.createdAt as createdAt, i.resolvedAt as resolvedAt
            from Incident i
            where i.resolvedAt is not null
              and (:reporterId is null or i.reporter.id = :reporterId)
            """)
    List<Timing> findResolvedTimings(@Param("reporterId") String reporterId);

    @Query("""
            select i.createdAt as createdAt, i.resolvedAt as resolvedAt
            from Incident i
            where (:reporterId is null or i.reporter.id = :reporterId)
              and (i.createdAt >= :since or i.resolvedAt >= :since)
            """)
    List<Timing> findTimingsSince(@Param("reporterId") String reporterId, @Param("since") Instant since);

    /** Projection for the status breakdown on the dashboard. */
    interface StatusCount {
        Status getStatus();

        long getCount();
    }

    /** Projection for resolution-time / trend calculations. */
    interface Timing {
        Instant getCreatedAt();

        Instant getResolvedAt();
    }
}
