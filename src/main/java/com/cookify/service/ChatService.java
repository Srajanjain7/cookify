package com.cookify.service;

import com.cookify.dto.ConversationSummaryResponse;
import com.cookify.exception.ApiException;
import com.cookify.model.ChatMessage;
import com.cookify.model.User;
import com.cookify.repository.ChatMessageRepository;
import com.cookify.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in chat, kept deliberately minimal: 1-to-1 text messages,
 * polling-friendly (no WebSocket infrastructure), no read receipts or
 * groups. See DESIGN-DEVIATIONS.md.
 */
@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public ChatMessage sendMessage(User sender, String recipientUsername, String text) {
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (recipient.getId().equals(sender.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't message yourself");
        }

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> conversation(User viewer, String otherUsername) {
        User other = userRepository.findByUsername(otherUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return chatMessageRepository.findConversation(viewer.getId(), other.getId());
    }

    /** One row per conversation partner, most recently active first. */
    public List<ConversationSummaryResponse> conversations(User viewer) {
        Map<String, ConversationSummaryResponse> byPartner = new LinkedHashMap<>();
        for (ChatMessage message : chatMessageRepository.findAllInvolving(viewer.getId())) {
            User other = message.getSender().getId().equals(viewer.getId()) ? message.getRecipient() : message.getSender();
            byPartner.putIfAbsent(other.getUsername(),
                    new ConversationSummaryResponse(other.getUsername(), message.getText(), message.getCreatedAt()));
        }
        return byPartner.values().stream().toList();
    }
}
