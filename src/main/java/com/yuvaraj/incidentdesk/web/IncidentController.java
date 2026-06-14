package com.yuvaraj.incidentdesk.web;

import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Role;
import com.yuvaraj.incidentdesk.domain.Status;
import com.yuvaraj.incidentdesk.dto.CommentDtos.AddCommentRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkDeleteRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkDeleteResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkUpdateRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.BulkUpdateResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.CreateIncidentRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.ListResult;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.Metrics;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.Stats;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.TriageRequest;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.TriageResponse;
import com.yuvaraj.incidentdesk.dto.IncidentDtos.UpdateIncidentRequest;
import com.yuvaraj.incidentdesk.exception.ApiException;
import com.yuvaraj.incidentdesk.ratelimit.RateLimiter;
import com.yuvaraj.incidentdesk.repository.IncidentFilter;
import com.yuvaraj.incidentdesk.security.AppUser;
import com.yuvaraj.incidentdesk.service.IncidentService;
import com.yuvaraj.incidentdesk.service.TriageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final TriageService triageService;
    private final RateLimiter rateLimiter;

    public IncidentController(IncidentService incidentService, TriageService triageService, RateLimiter rateLimiter) {
        this.incidentService = incidentService;
        this.triageService = triageService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public ListResult list(@RequestParam(required = false) Status status,
                           @RequestParam(required = false) IncidentType type,
                           @RequestParam(required = false) Priority priority,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String assigneeId,
                           @RequestParam(required = false) Boolean overdue,
                           @RequestParam(required = false) String cursor,
                           @RequestParam(defaultValue = "10") int limit,
                           @AuthenticationPrincipal AppUser user) {
        int clamped = Math.max(1, Math.min(limit, 50));
        String reporterScope = user.role() == Role.REPORTER ? user.id() : null;
        IncidentFilter filter = new IncidentFilter(status, type, priority,
                (q != null && !q.isBlank()) ? q.trim() : null, assigneeId, overdue, reporterScope);
        return incidentService.list(filter, cursor, clamped);
    }

    @GetMapping("/stats")
    public Stats stats(@AuthenticationPrincipal AppUser user) {
        return incidentService.getStats(user);
    }

    @GetMapping("/metrics")
    public Metrics metrics(@AuthenticationPrincipal AppUser user) {
        return incidentService.getMetrics(user);
    }

    @GetMapping("/triage/status")
    public Map<String, Object> triageStatus() {
        return Map.of("enabled", triageService.isEnabled());
    }

    @PostMapping("/triage")
    public TriageResponse triage(@Valid @RequestBody TriageRequest request, HttpServletRequest http) {
        rateLimiter.check("triage:" + http.getRemoteAddr(), 15, 60);
        return triageService.triage(request);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateIncidentRequest request,
                                                      HttpServletRequest http,
                                                      @AuthenticationPrincipal AppUser user) {
        rateLimiter.check("write:" + http.getRemoteAddr(), 60, 60);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("incident", incidentService.create(request, user)));
    }

    @PostMapping("/bulk-update")
    public BulkUpdateResult bulkUpdate(@Valid @RequestBody BulkUpdateRequest request,
                                       HttpServletRequest http,
                                       @AuthenticationPrincipal AppUser user) {
        rateLimiter.check("write:" + http.getRemoteAddr(), 60, 60);
        return incidentService.bulkUpdate(request, user);
    }

    @PostMapping("/bulk-delete")
    public BulkDeleteResult bulkDelete(@Valid @RequestBody BulkDeleteRequest request, HttpServletRequest http) {
        rateLimiter.check("write:" + http.getRemoteAddr(), 60, 60);
        return incidentService.bulkDelete(request);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id, @AuthenticationPrincipal AppUser user) {
        return Map.of("incident", incidentService.getDetail(id, user));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id,
                                      @RequestBody UpdateIncidentRequest request,
                                      @AuthenticationPrincipal AppUser user) {
        boolean assigneeProvided = request.assigneeId() != null && request.assigneeId().isPresent();
        boolean dueProvided = request.dueDate() != null && request.dueDate().isPresent();
        if (request.status() == null && request.priority() == null && !assigneeProvided && !dueProvided) {
            throw ApiException.badRequest("At least one field is required");
        }
        return Map.of("incident", incidentService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        incidentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public Map<String, Object> listComments(@PathVariable String id, @AuthenticationPrincipal AppUser user) {
        return Map.of("comments", incidentService.listComments(id, user));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable String id,
                                                          @Valid @RequestBody AddCommentRequest request,
                                                          HttpServletRequest http,
                                                          @AuthenticationPrincipal AppUser user) {
        rateLimiter.check("write:" + http.getRemoteAddr(), 60, 60);
        boolean internal = Boolean.TRUE.equals(request.internal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("comment", incidentService.addComment(id, request.body(), internal, user)));
    }
}
