package com.cookify.repository;

import com.cookify.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);
    long countByCreatorId(Long creatorId);
    List<Subscription> findByCreatorId(Long creatorId);
    List<Subscription> findBySubscriberId(Long subscriberId);
}
