package com.cookify.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = "Comment text is required") String text
) {
}
