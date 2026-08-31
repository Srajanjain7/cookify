package com.cookify.controller;

import com.cookify.dto.ApiResponse;
import com.cookify.dto.ChatMessageRequest;
import com.cookify.dto.ChatMessageResponse;
import com.cookify.dto.ConversationSummaryResponse;
import com.cookify.security.CookifyUserDetails;
import com.cookify.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** All endpoints require auth via SecurityConfig's default anyRequest().authenticated(). */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationSummaryResponse>> conversations(@AuthenticationPrincipal CookifyUserDetails principal) {
        return ApiResponse.ok("OK", chatService.conversations(principal.getUser()));
    }

    @GetMapping("/{username}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@AuthenticationPrincipal CookifyUserDetails principal,
                                                             @PathVariable String username) {
        List<ChatMessageResponse> messages = chatService.conversation(principal.getUser(), username).stream()
                .map(ChatMessageResponse::from)
                .toList();
        return ApiResponse.ok("OK", messages);
    }

    @PostMapping("/{username}/messages")
    public ApiResponse<ChatMessageResponse> send(@AuthenticationPrincipal CookifyUserDetails principal,
                                                  @PathVariable String username,
                                                  @Valid @RequestBody ChatMessageRequest request) {
        var message = chatService.sendMessage(principal.getUser(), username, request.text());
        return ApiResponse.ok("Sent", ChatMessageResponse.from(message));
    }
}
