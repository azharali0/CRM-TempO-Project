CREATE TABLE email_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(150),
    subject VARCHAR(200),
    status VARCHAR(20),
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT NOW()
);
