package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.PublicProfileResponse;
import com.cookify.dto.RecipeSummaryResponse;
import com.cookify.exception.ApiException;
import com.cookify.model.User;
import com.cookify.repository.UserRepository;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.RecipeService;
import com.cookify.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public profile viewing + subscribe/unsubscribe (test case 13). GET
 * is public via SecurityConfig; subscribe/unsubscribe require auth.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final RecipeService recipeService;

    public UserController(UserRepository userRepository, SubscriptionService subscriptionService, RecipeService recipeService) {
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.recipeService = recipeService;
    }

    @GetMapping("/{username}")
    public ApiResponse<PublicProfileResponse> profile(@AuthenticationPrincipal CookifyUserDetails principal,
                                                        @PathVariable String username) {
        User user = findByUsername(username);
        Long viewerId = principal == null ? null : principal.getUser().getId();
        Boolean subscribedByMe = principal == null ? null : subscriptionService.isSubscribed(viewerId, user.getId());

        return ApiResponse.ok("OK", PublicProfileResponse.from(
                user,
                subscriptionService.followerCount(user.getId()),
                subscriptionService.followingCount(user.getId()),
                recipeService.countByCreator(user.getId()),
                subscribedByMe));
    }

    @GetMapping("/{username}/recipes")
    public ApiResponse<List<RecipeSummaryResponse>> recipes(@PathVariable String username) {
        User user = findByUsername(username);
        return ApiResponse.ok("OK", recipeService.listByCreator(user.getId()));
    }

    @PostMapping("/{username}/subscribe")
    public ApiResponse<Void> subscribe(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable String username) {
        subscriptionService.subscribe(principal.getUser(), username);
        return ApiResponse.ok("Subscribed");
    }

    @DeleteMapping("/{username}/subscribe")
    public ApiResponse<Void> unsubscribe(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable String username) {
        subscriptionService.unsubscribe(principal.getUser(), username);
        return ApiResponse.ok("Unsubscribed");
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
