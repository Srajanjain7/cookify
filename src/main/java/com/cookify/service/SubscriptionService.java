package com.cookify.service;

import com.cookify.exception.ApiException;
import com.cookify.model.Subscription;
import com.cookify.model.User;
import com.cookify.repository.SubscriptionRepository;
import com.cookify.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subscription, following the assignment's Subscription pseudocode
 * ("INCREASE Subscriber Count for Recipe Owner... NOTIFY Recipe Owner
 * via Email"). The pseudocode's validateCommentSafe() gate is not
 * implemented -- there is no comment/text input anywhere in a
 * subscribe action to validate; see DESIGN-DEVIATIONS.md.
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                UserRepository userRepository,
                                MailService mailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void subscribe(User subscriber, String creatorUsername) {
        User creator = resolveCreator(subscriber, creatorUsername);

        if (subscriptionRepository.findBySubscriberIdAndCreatorId(subscriber.getId(), creator.getId()).isPresent()) {
            return; // already subscribed -- idempotent, no duplicate row or repeat email
        }

        Subscription subscription = new Subscription();
        subscription.setSubscriber(subscriber);
        subscription.setCreator(creator);
        subscriptionRepository.save(subscription);

        mailService.send(creator.getEmail(), "You have a new subscriber",
                subscriber.getUsername() + " subscribed to your recipes on COOKify!");
    }

    @Transactional
    public void unsubscribe(User subscriber, String creatorUsername) {
        User creator = resolveCreator(subscriber, creatorUsername);
        subscriptionRepository.findBySubscriberIdAndCreatorId(subscriber.getId(), creator.getId())
                .ifPresent(subscriptionRepository::delete);
    }

    public boolean isSubscribed(Long subscriberId, Long creatorId) {
        if (subscriberId == null) {
            return false;
        }
        return subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId).isPresent();
    }

    public long followerCount(Long creatorId) {
        return subscriptionRepository.countByCreatorId(creatorId);
    }

    public long followingCount(Long subscriberId) {
        return subscriptionRepository.countBySubscriberId(subscriberId);
    }

    private User resolveCreator(User subscriber, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (creator.getId().equals(subscriber.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't subscribe to yourself");
        }
        return creator;
    }
}
