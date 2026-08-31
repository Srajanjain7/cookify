package com.cookify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default mail "sender" -- no SMTP credentials exist for this project
 * yet, so emails are logged instead of sent. This keeps every email
 * driven flow (2FA codes, password reset, comment/rating/subscription
 * notifications) fully testable end-to-end right now. Switch to real
 * SMTP by setting app.mail.mode=smtp and the spring.mail.* properties
 * in application.properties.
 */
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[MAIL:LOG-MODE] To: {} | Subject: {} | Body: {}", to, subject, body);
    }
}
