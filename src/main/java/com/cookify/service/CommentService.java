package com.cookify.service;

import com.cookify.exception.ApiException;
import com.cookify.model.Comment;
import com.cookify.model.User;
import com.cookify.model.recipe.Recipe;
import com.cookify.repository.CommentRepository;
import com.cookify.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Commenting, following the assignment's Commenting pseudocode. */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final ModerationService moderationService;
    private final MailService mailService;

    public CommentService(CommentRepository commentRepository,
                           RecipeRepository recipeRepository,
                           ModerationService moderationService,
                           MailService mailService) {
        this.commentRepository = commentRepository;
        this.recipeRepository = recipeRepository;
        this.moderationService = moderationService;
        this.mailService = mailService;
    }

    @Transactional
    public Comment addComment(User author, Long recipeId, String text) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipe not found"));

        if (!moderationService.isSafe(text)) {
            moderationService.issueWarning(author, "Posted a comment containing inappropriate language");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Comment: Please use appropriate language");
        }

        Comment comment = new Comment();
        comment.setRecipe(recipe);
        comment.setAuthor(author);
        comment.setText(text);
        comment = commentRepository.save(comment);

        if (!recipe.getCreator().getId().equals(author.getId())) {
            mailService.send(recipe.getCreator().getEmail(), "New comment on your recipe",
                    author.getUsername() + " commented on \"" + recipe.getRecipeName() + "\": " + text);
        }

        return comment;
    }

    public List<Comment> listComments(Long recipeId) {
        return commentRepository.findByRecipeIdOrderByCreatedAtAsc(recipeId);
    }
}
