package com.crm.service;

import com.crm.model.entity.EmailLog;
import com.crm.repository.EmailLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );
    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_MS = {1000L, 5000L, 15000L};

    private final EmailLogRepository emailLogRepository;
    private final JavaMailSender mailSender;
    private final boolean mailAvailable;

    public EmailService(EmailLogRepository emailLogRepository,
                        JavaMailSender mailSender) {
        this.emailLogRepository = emailLogRepository;
        boolean available = false;
        try {
            if (mailSender != null) {
                available = true;
            }
        } catch (Exception e) {
            log.warn("JavaMailSender not available — email sending disabled: {}", e.getMessage());
        }
        this.mailSender = mailSender;
        this.mailAvailable = available;
    }

    @Async("taskExecutor")
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    public void sendEmail(String to, String subject, String body) {
        try {
            if (!mailAvailable) {
                log.warn("Mail not configured — skipping email to [REDACTED], subject: {}", subject);
                logEmail(to, subject, "FAILED", "Mail sender not configured");
                return;
            }

            if (!isValidEmail(to)) {
                log.warn("Invalid email address — skipping send for subject: {}", subject);
                logEmail(to, subject, "FAILED", "Invalid email address");
                return;
            }

            Exception lastException = null;
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    if (attempt > 0) {
                        logEmail(to, subject, "RETRYING", "Retry attempt " + (attempt + 1));
                        Thread.sleep(BACKOFF_MS[attempt]);
                    }
                    doSend(to, subject, body);
                    logEmail(to, subject, "SENT", null);
                    log.info("Email sent successfully — subject: {}", subject);
                    return;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logEmail(to, subject, "FAILED", "Interrupted during retry backoff");
                    return;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("Email send attempt {} failed — subject: {}, error: {}",
                            attempt + 1, subject, e.getMessage());
                }
            }

            String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
            logEmail(to, subject, "FAILED", errorMsg);
            log.error("Email permanently failed after {} retries — subject: {}", MAX_RETRIES, subject);
        } catch (Exception e) {
            log.error("Unexpected error in email sending pipeline — subject: {}, error: {}",
                    subject, e.getMessage());
            try {
                logEmail(to, subject, "FAILED", "Unexpected: " + e.getMessage());
            } catch (Exception logEx) {
                log.error("Failed to log email failure: {}", logEx.getMessage());
            }
        }
    }

    private void doSend(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (email.contains("\n") || email.contains("\r") || email.contains("%0a") || email.contains("%0d")) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void logEmail(String recipientEmail, String subject, String status, String errorMessage) {
        try {
            EmailLog emailLog = EmailLog.builder()
                    .recipientEmail(recipientEmail)
                    .subject(truncate(subject, 200))
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            log.error("Failed to persist email log: {}", e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * Circuit breaker fallback — logs failure when SMTP is completely unreachable.
     */
    @SuppressWarnings("unused")
    private void sendEmailFallback(String to, String subject, String body, Throwable t) {
        log.error("Circuit breaker OPEN for email service — subject: {}, error: {}", subject, t.getMessage());
        logEmail(to, subject, "FAILED", "Circuit breaker open: " + t.getMessage());
    }
}
