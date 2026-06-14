package com.yuvaraj.incidentdesk.service;

import com.yuvaraj.incidentdesk.domain.AuditLog;
import com.yuvaraj.incidentdesk.domain.Comment;
import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Role;
import com.yuvaraj.incidentdesk.domain.Status;
import com.yuvaraj.incidentdesk.domain.User;
import com.yuvaraj.incidentdesk.dto.CommentDtos.CommentResponse;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkDeleteRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkDeleteResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkUpdateRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkUpdateResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.CreateIncidentRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.IncidentResponse;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.ListResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.Metrics;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.Stats;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.TrendPoint;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.UpdateIncidentRequest;
import com.yuvaraj.incidentdesk.exception.ApiException;
import com.yuvaraj.incidentdesk.realtime.RealtimeService;
import com.yuvaraj.incidentdesk.repository.AuditLogRepository;
import com.yuvaraj.incidentdesk.repository.CommentRepository;
import com.yuvaraj.incidentdesk.repository.IncidentFilter;
import com.yuvaraj.incidentdesk.repository.IncidentRepository;
import com.yuvaraj.incidentdesk.repository.IncidentSpecifications;
import com.yuvaraj.incidentdesk.repository.UserRepository;
import com.yuvaraj.incidentdesk.security.AppUser;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private static final int TREND_DAYS = 14;

    private final IncidentRepository incidents;
    private final AuditLogRepository auditLogs;
    private final CommentRepository comments;
    private final UserRepository users;
    private final RealtimeService realtime;

    public IncidentService(IncidentRepository incidents, AuditLogRepository auditLogs, CommentRepository comments,
                           UserRepository users, RealtimeService realtime) {
        this.incidents = incidents;
        this.auditLogs = auditLogs;
        this.comments = comments;
        this.users = users;
        this.realtime = realtime;
    }

    // ---- Create -------------------------------------------------------------

    @Transactional
    public IncidentResponse create(CreateIncidentRequest input, AppUser actor) {
        User reporter = users.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("Session user no longer exists"));
        Incident incident = new Incident();
        incident.setTitle(input.title().trim());
        incident.setType(input.type() == null ? IncidentType.INCIDENT : input.type());
        incident.setPriority(input.priority() == null ? Priority.MEDIUM : input.priority());
        incident.setDescription(StringUtils.hasText(input.description()) ? input.description().trim() : null);
        incident.setReporter(reporter);
        Incident saved = incidents.save(incident);
        realtime.emitIncidentsChanged();
        return IncidentResponse.summary(saved);
    }

    // ---- Read ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Stats getStats(AppUser user) {
        String reporterId = user.role() == Role.REPORTER ? user.id() : null;
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Status s : Status.values()) {
            byStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (IncidentRepository.StatusCount row : incidents.countByStatus(reporterId)) {
            byStatus.put(row.getStatus().name(), row.getCount());
            total += row.getCount();
        }
        return new Stats(total, byStatus);
    }

    @Transactional(readOnly = true)
    public ListResult list(IncidentFilter filter, String cursor, int limit) {
        Specification<Incident> filterSpec = IncidentSpecifications.matching(filter);
        long total = incidents.count(filterSpec);

        Specification<Incident> pageSpec = filterSpec.and(IncidentSpecifications.withRelations());
        if (cursor != null) {
            Incident cursorIncident = incidents.findById(cursor).orElse(null);
            if (cursorIncident != null) {
                pageSpec = pageSpec.and(IncidentSpecifications.createdBefore(cursorIncident.getCreatedAt()));
            }
        }

        PageRequest page = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Incident> rows = incidents.findAll(pageSpec, page).getContent();

        boolean hasMore = rows.size() > limit;
        List<Incident> items = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? items.get(items.size() - 1).getId() : null;

        List<IncidentResponse> mapped = items.stream().map(IncidentResponse::summary).toList();
        return new ListResult(mapped, nextCursor, total);
    }

    @Transactional(readOnly = true)
    public Metrics getMetrics(AppUser user) {
        String reporterId = user.role() == Role.REPORTER ? user.id() : null;

        List<IncidentRepository.Timing> resolved = incidents.findResolvedTimings(reporterId);
        Double mttrHours = null;
        if (!resolved.isEmpty()) {
            double totalHours = resolved.stream()
                    .mapToDouble(t -> (t.getResolvedAt().toEpochMilli() - t.getCreatedAt().toEpochMilli()) / 3_600_000.0)
                    .sum();
            mttrHours = totalHours / resolved.size();
        }

        Instant since = Instant.now().atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
                .minusDays(TREND_DAYS - 1L).toInstant();

        Map<String, TrendBucket> buckets = new LinkedHashMap<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            String key = dayKey(since.plus(i, ChronoUnit.DAYS));
            buckets.put(key, new TrendBucket(key));
        }

        for (IncidentRepository.Timing row : incidents.findTimingsSince(reporterId, since)) {
            TrendBucket created = buckets.get(dayKey(row.getCreatedAt()));
            if (created != null) {
                created.created++;
            }
            if (row.getResolvedAt() != null) {
                TrendBucket res = buckets.get(dayKey(row.getResolvedAt()));
                if (res != null) {
                    res.resolved++;
                }
            }
        }

        List<TrendPoint> trend = buckets.values().stream()
                .map(b -> new TrendPoint(b.date, b.created, b.resolved))
                .toList();
        return new Metrics(mttrHours, resolved.size(), trend);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getDetail(String id, AppUser user) {
        Incident incident = requireVisible(id, user);
        List<AuditLog> audit = auditLogs.findByIncidentIdOrderByCreatedAtDesc(id);
        return IncidentResponse.detail(incident, audit);
    }

    // ---- Update (admin) -----------------------------------------------------

    @Transactional
    public IncidentResponse update(String id, UpdateIncidentRequest input, AppUser actor) {
        Incident existing = incidents.findById(id)
                .orElseThrow(() -> ApiException.notFound("Incident not found"));

        if (input.expectedUpdatedAt() != null) {
            long expected = parseInstant(input.expectedUpdatedAt()).toEpochMilli();
            if (expected != existing.getUpdatedAt().toEpochMilli()) {
                throw ApiException.conflict("This incident was changed by someone else. Refresh and try again.");
            }
        }

        boolean assigneeProvided = input.assigneeId() != null && input.assigneeId().isPresent();
        String newAssigneeId = assigneeProvided ? input.assigneeId().get() : null;
        if (assigneeProvided && newAssigneeId != null && !users.existsById(newAssigneeId)) {
            throw ApiException.badRequest("Assignee does not exist");
        }

        User actorUser = users.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("Session user no longer exists"));
        List<AuditLog> entries = new ArrayList<>();

        if (input.status() != null && input.status() != existing.getStatus()) {
            if (!com.yuvaraj.incidentdesk.util.StatusTransitions.canTransition(existing.getStatus(), input.status())) {
                throw ApiException.conflict("Cannot change status from " + existing.getStatus() + " to " + input.status());
            }
            entries.add(new AuditLog(existing, actorUser, "status", existing.getStatus().name(), input.status().name()));
            existing.setStatus(input.status());

            boolean closedOut = input.status() == Status.RESOLVED || input.status() == Status.CLOSED;
            if (closedOut && existing.getResolvedAt() == null) {
                existing.setResolvedAt(Instant.now());
            } else if (!closedOut && existing.getResolvedAt() != null) {
                existing.setResolvedAt(null);
            }
        }

        if (input.priority() != null && input.priority() != existing.getPriority()) {
            entries.add(new AuditLog(existing, actorUser, "priority", existing.getPriority().name(), input.priority().name()));
            existing.setPriority(input.priority());
        }

        if (assigneeProvided) {
            String currentAssigneeId = existing.getAssignee() == null ? null : existing.getAssignee().getId();
            if (!java.util.Objects.equals(newAssigneeId, currentAssigneeId)) {
                entries.add(new AuditLog(existing, actorUser, "assignee", currentAssigneeId, newAssigneeId));
                existing.setAssignee(newAssigneeId == null ? null : users.getReferenceById(newAssigneeId));
            }
        }

        if (input.dueDate() != null && input.dueDate().isPresent()) {
            String raw = input.dueDate().get();
            Instant newDue = raw == null ? null : parseInstant(raw);
            Long currentMs = existing.getDueDate() == null ? null : existing.getDueDate().toEpochMilli();
            Long newMs = newDue == null ? null : newDue.toEpochMilli();
            if (!java.util.Objects.equals(currentMs, newMs)) {
                entries.add(new AuditLog(existing, actorUser, "dueDate",
                        existing.getDueDate() == null ? null : existing.getDueDate().toString(),
                        newDue == null ? null : newDue.toString()));
                existing.setDueDate(newDue);
            }
        }

        if (entries.isEmpty()) {
            // Nothing changed — return current detail without writing a no-op audit row.
            return IncidentResponse.detail(existing, auditLogs.findByIncidentIdOrderByCreatedAtDesc(id));
        }

        incidents.save(existing);
        auditLogs.saveAll(entries);
        realtime.emitIncidentsChanged();
        return IncidentResponse.detail(existing, auditLogs.findByIncidentIdOrderByCreatedAtDesc(id));
    }

    // ---- Delete (admin) -----------------------------------------------------

    @Transactional
    public void delete(String id) {
        Incident existing = incidents.findById(id)
                .orElseThrow(() -> ApiException.notFound("Incident not found"));
        incidents.delete(existing);
        realtime.emitIncidentsChanged();
    }

    @Transactional
    public BulkUpdateResult bulkUpdate(BulkUpdateRequest input, AppUser actor) {
        if (input.assigneeId() != null && input.assigneeId().isPresent()) {
            String assigneeId = input.assigneeId().get();
            if (assigneeId != null && !users.existsById(assigneeId)) {
                throw ApiException.badRequest("Assignee does not exist");
            }
        }
        UpdateIncidentRequest patch = new UpdateIncidentRequest(
                input.status(), input.priority(),
                input.assigneeId() == null ? JsonNullable.undefined() : input.assigneeId(),
                JsonNullable.undefined(), null);

        long updated = 0;
        long skipped = 0;
        for (String id : input.ids()) {
            try {
                update(id, patch, actor);
                updated++;
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        realtime.emitIncidentsChanged();
        return new BulkUpdateResult(updated, skipped);
    }

    @Transactional
    public BulkDeleteResult bulkDelete(BulkDeleteRequest input) {
        List<Incident> found = incidents.findAllById(input.ids());
        incidents.deleteAll(found);
        realtime.emitIncidentsChanged();
        return new BulkDeleteResult(found.size());
    }

    // ---- Comments -----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(String id, AppUser user) {
        requireVisible(id, user);
        List<Comment> rows = user.role() == Role.ADMIN
                ? comments.findByIncidentIdOrderByCreatedAtAsc(id)
                : comments.findByIncidentIdAndInternalFalseOrderByCreatedAtAsc(id);
        return rows.stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse addComment(String id, String body, boolean internal, AppUser user) {
        Incident incident = requireVisible(id, user);
        boolean isInternal = internal && user.role() == Role.ADMIN;
        User author = users.getReferenceById(user.id());
        Comment comment = comments.save(new Comment(incident, author, body.trim(), isInternal));
        realtime.emitIncidentsChanged();
        return CommentResponse.from(comment);
    }

    // ---- Helpers ------------------------------------------------------------

    private Incident requireVisible(String id, AppUser user) {
        Incident incident = incidents.findById(id)
                .orElseThrow(() -> ApiException.notFound("Incident not found"));
        // Return 404 (not 403) for non-owned records so we don't leak existence.
        if (user.role() == Role.REPORTER && !incident.getReporter().getId().equals(user.id())) {
            throw ApiException.notFound("Incident not found");
        }
        return incident;
    }

    private static String dayKey(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC).toLocalDate().toString();
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (Exception e) {
                throw ApiException.badRequest("Invalid date: " + value);
            }
        }
    }

    private static final class TrendBucket {
        final String date;
        long created;
        long resolved;

        TrendBucket(String date) {
            this.date = date;
        }
    }
}
