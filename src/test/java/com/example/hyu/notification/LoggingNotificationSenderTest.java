package com.example.hyu.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggingNotificationSenderTest {

    private LoggingNotificationSender sender;
    private Logger underlyingLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        sender = new LoggingNotificationSender();
        underlyingLogger = (Logger) LoggerFactory.getLogger(LoggingNotificationSender.class);
        // Ensure INFO is enabled
        underlyingLogger.setLevel(Level.INFO);

        listAppender = new ListAppender<>();
        listAppender.start();
        underlyingLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (underlyingLogger \!= null && listAppender \!= null) {
            underlyingLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    @DisplayName("sendInApp logs formatted INFO message for typical inputs")
    void sendInApp_logsInfo_withExpectedFormat() {
        Long userId = 42L;
        String title = "Welcome";
        String body = "Hello there";

        sender.sendInApp(userId, title, body);

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size(), "Exactly one log event should be emitted");
        ILoggingEvent e = events.get(0);
        assertEquals(Level.INFO, e.getLevel(), "Log level should be INFO");
        assertEquals(LoggingNotificationSender.class.getName(), e.getLoggerName(), "Logger name should match class");
        assertEquals("[IN-APP] userId=42 | Welcome - Hello there", e.getFormattedMessage(), "Message format should match");
    }

    @Test
    @DisplayName("sendInApp tolerates nulls and logs 'null' placeholders")
    void sendInApp_handlesNulls() {
        sender.sendInApp(null, null, "Body");

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size(), "One log event expected");
        ILoggingEvent e = events.get(0);
        assertEquals(Level.INFO, e.getLevel(), "Should still log at INFO");
        assertEquals("[IN-APP] userId=null | null - Body", e.getFormattedMessage(), "Nulls should be rendered as 'null'");
    }

    @Test
    @DisplayName("sendInApp logs long strings without truncation")
    void sendInApp_logsLongStrings() {
        Long userId = 7L;
        String title = "T".repeat(2000);
        String body = "B".repeat(3000);

        sender.sendInApp(userId, title, body);

        List<ILoggingEvent> events = listAppender.list;
        assertEquals(1, events.size(), "One log event expected");
        ILoggingEvent e = events.get(0);
        String msg = e.getFormattedMessage();

        assertTrue(msg.startsWith("[IN-APP] userId=7 | "), "Prefix should match");
        assertTrue(msg.contains(" - "), "Separator should be present");
        assertTrue(msg.contains(title), "Title should be fully present");
        assertTrue(msg.endsWith(body), "Body should be fully present at the end");
        assertEquals(Level.INFO, e.getLevel(), "Level should be INFO");
    }
}