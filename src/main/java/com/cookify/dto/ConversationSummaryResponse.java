package com.cookify.dto;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(String otherUsername, String lastMessage, LocalDateTime lastMessageAt) {
}
