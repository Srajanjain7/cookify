package com.cookify.service;

import com.cookify.model.AccountStatus;
import com.cookify.model.User;
import com.cookify.model.Warning;
import com.cookify.model.recipe.Recipe;
import com.cookify.repository.ChatMessageRepository;
import com.cookify.repository.CommentRepository;
import com.cookify.repository.RatingRepository;
import com.cookify.repository.RecipeRepository;
import com.cookify.repository.SavedRecipeRepository;
import com.cookify.repository.SubscriptionRepository;
import com.cookify.repository.UserRepository;
import com.cookify.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * NSFW filtering (Commenting pseudocode's validateComment /
 * Subscription pseudocode's validateCommentSafe -- both map to the
 * one isSafe() check here), warnings, and the 3-warning ban cascade
 * (test case 11).
 */
@Service
public class ModerationService {

    private static final int WARNINGS_BEFORE_BAN = 3;

    /**
     * A small representative keyword blocklist, not a real NSFW/ML
     * moderation model -- no such model or word list is specified
     * anywhere in the assignment. Swap for a real moderation API in
     * production. See DESIGN-DEVIATIONS.md.
     */
    private static final Set<String> BLOCKED_TERMS = Set.of(
            "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "slut", "whore"
    );

    private final UserRepository userRepository;
    private final WarningRepository warningRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RecipeRepository recipeRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MailService mailService;

    public ModerationService(UserRepository userRepository,
                              WarningRepository warningRepository,
                              CommentRepository commentRepository,
                              RatingRepository ratingRepository,
                              SavedRecipeRepository savedRecipeRepository,
                              SubscriptionRepository subscriptionRepository,
                              RecipeRepository recipeRepository,
                              ChatMessageRepository chatMessageRepository,
                              MailService mailService) {
        this.userRepository = userRepository;
        this.warningRepository = warningRepository;
        this.commentRepository = commentRepository;
        this.ratingRepository = ratingRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.recipeRepository = recipeRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.mailService = mailService;
    }

    public boolean isSafe(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String lower = text.toLowerCase();
        return BLOCKED_TERMS.stream().noneMatch(lower::contains);
    }

    /**
     * Emails the user a warning, stores it (test case 8: "warning is
     * stored in the user database"), and bans the account once the
     * 3-warning threshold is reached (test case 11).
     *
     * REQUIRES_NEW: callers (e.g. CommentService.addComment) reject the
     * triggering content by throwing after this returns, inside their
     * own @Transactional method. Without a separate transaction here,
     * that exception would roll back this warning/ban right along with
     * the rejected comment -- exactly the content that shouldn't
     * survive, taking with it the record that should.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueWarning(User user, String reason) {
        Warning warning = new Warning();
        warning.setUser(user);
        warning.setReason(reason);
        warningRepository.save(warning);

        user.setWarningCount(user.getWarningCount() + 1);
        userRepository.save(user);

        mailService.send(user.getEmail(), "Content warning on your COOKify account",
                "Your recent activity violated our content guidelines: " + reason + ". "
                        + "This is warning " + user.getWarningCount() + " of " + WARNINGS_BEFORE_BAN
                        + " -- accounts are banned after " + WARNINGS_BEFORE_BAN + " warnings.");

        if (user.getWarningCount() >= WARNINGS_BEFORE_BAN && user.getAccountStatus() == AccountStatus.ACTIVE) {
            banUser(user);
        }
    }

    /**
     * Test case 11: "removing all comments, recipes uploaded, ratings
     * and interaction history from the database." Warnings themselves
     * are kept as the audit trail for why the ban happened.
     */
    private void banUser(User user) {
        user.setAccountStatus(AccountStatus.BANNED);
        userRepository.save(user);

        List<Recipe> ownRecipes = recipeRepository.findByCreatorId(user.getId());
        List<Long> ownRecipeIds = ownRecipes.stream().map(Recipe::getId).toList();

        if (!ownRecipeIds.isEmpty()) {
            commentRepository.deleteByRecipeIdIn(ownRecipeIds);
            ratingRepository.deleteByRecipeIdIn(ownRecipeIds);
            savedRecipeRepository.deleteByRecipeIdIn(ownRecipeIds);
        }
        commentRepository.deleteByAuthorId(user.getId());
        ratingRepository.deleteByUserId(user.getId());
        savedRecipeRepository.deleteByUserId(user.getId());
        subscriptionRepository.deleteBySubscriberId(user.getId());
        subscriptionRepository.deleteByCreatorId(user.getId());
        chatMessageRepository.deleteBySenderId(user.getId());
        chatMessageRepository.deleteByRecipientId(user.getId());

        if (!ownRecipes.isEmpty()) {
            recipeRepository.deleteAll(ownRecipes);
        }

        mailService.send(user.getEmail(), "Your COOKify account has been banned",
                "Your account has been banned after receiving " + WARNINGS_BEFORE_BAN + " content warnings. "
                        + "All your recipes, comments, ratings, and interaction history have been removed.");
    }
}
