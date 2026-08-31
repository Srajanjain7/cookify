package com.cookify.repository;

import com.cookify.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m where "
            + "(m.sender.id = :userId1 and m.recipient.id = :userId2) "
            + "or (m.sender.id = :userId2 and m.recipient.id = :userId1) "
            + "order by m.createdAt asc")
    List<ChatMessage> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("select m from ChatMessage m where m.sender.id = :userId or m.recipient.id = :userId "
            + "order by m.createdAt desc")
    List<ChatMessage> findAllInvolving(@Param("userId") Long userId);

    /** Ban cascade (test case 11): chat history is interaction history too. */
    void deleteBySenderId(Long senderId);
    void deleteByRecipientId(Long recipientId);
}
