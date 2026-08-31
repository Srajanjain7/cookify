package com.cookify.repository;

import com.cookify.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipeIdOrderByCreatedAtAsc(Long recipeId);

    /** Ban cascade (test case 11): remove content authored by the banned user, and content on their recipes. */
    void deleteByAuthorId(Long authorId);
    void deleteByRecipeIdIn(Collection<Long> recipeIds);
}
