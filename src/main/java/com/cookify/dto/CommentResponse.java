package com.cookify.dto;

import com.cookify.model.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String text,
        Long authorId,
        String authorUsername,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getCreatedAt()
        );
    }
}
