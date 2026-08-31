package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.CommentCreateRequest;
import com.cookify.dto.CommentResponse;
import com.cookify.model.Comment;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Commenting (test case 8). GET is public; POST requires auth via SecurityConfig. */
@RestController
@RequestMapping("/api/recipes/{recipeId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(@PathVariable Long recipeId) {
        List<CommentResponse> comments = commentService.listComments(recipeId).stream()
                .map(CommentResponse::from)
                .toList();
        return ApiResponse.ok("OK", comments);
    }

    @PostMapping
    public ApiResponse<CommentResponse> create(@AuthenticationPrincipal CookifyUserDetails principal,
                                                @PathVariable Long recipeId,
                                                @Valid @RequestBody CommentCreateRequest request) {
        Comment comment = commentService.addComment(principal.getUser(), recipeId, request.text());
        return ApiResponse.ok("Comment posted", CommentResponse.from(comment));
    }
}
