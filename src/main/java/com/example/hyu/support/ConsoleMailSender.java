package com.example.hyu.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class ConsoleMailSender implements MailSender {
    @Override public void send(String to, String subject, String body) {
        log.info("[MAIL] to={}, subject={}, body=\n{}", to, subject, body);
    }
}
