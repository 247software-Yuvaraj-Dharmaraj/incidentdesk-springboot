package com.yuvaraj.incidentdesk.repository;

import com.yuvaraj.incidentdesk.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {

    List<Comment> findByIncidentIdOrderByCreatedAtAsc(String incidentId);

    List<Comment> findByIncidentIdAndInternalFalseOrderByCreatedAtAsc(String incidentId);
}
