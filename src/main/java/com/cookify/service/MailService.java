package com.cookify.service;

public interface MailService {
    void send(String to, String subject, String body);
}
