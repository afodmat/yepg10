package com.yep.platform.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Development mail sender: writes the message to the log instead of sending it.
 * Replace with an SMTP/API implementation (same interface) for production.
 */
@Service
public class LoggingMailService implements MailService {
    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);
    @Override public void send(String to, String subject, String body) {
        log.info("[MAIL] to={} subject={}\n{}", to, subject, body);
    }
}
