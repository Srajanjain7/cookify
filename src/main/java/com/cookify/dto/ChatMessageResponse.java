package com.cookify.dto;

import com.cookify.model.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String senderUsername,
        String recipientUsername,
        String text,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSender().getUsername(),
                message.getRecipient().getUsername(),
                message.getText(),
                message.getCreatedAt()
        );
    }
}
