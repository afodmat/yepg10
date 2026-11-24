package com.yep.platform.mail;

public interface MailService {
    void send(String to, String subject, String body);
}
