package com.yuvaraj.incidentdesk.repository;

import com.yuvaraj.incidentdesk.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByIncidentIdOrderByCreatedAtDesc(String incidentId);
}
