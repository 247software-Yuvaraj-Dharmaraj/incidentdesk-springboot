package com.yuvaraj.incidentdesk.dto;

import com.yuvaraj.incidentdesk.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record AddCommentRequest(
            @NotBlank(message = "Comment cannot be empty") @Size(max = 2000) String body,
            Boolean internal) {
    }

    public record CommentResponse(
            String id,
            String incidentId,
            String authorId,
            String body,
            boolean internal,
            Instant createdAt,
            AuthDtos.UserPreview author) {

        public static CommentResponse from(Comment c) {
            return new CommentResponse(
                    c.getId(),
                    c.getIncident().getId(),
                    c.getAuthor().getId(),
                    c.getBody(),
                    c.isInternal(),
                    c.getCreatedAt(),
                    AuthDtos.UserPreview.from(c.getAuthor()));
        }
    }
}
