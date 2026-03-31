CREATE TABLE interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    type VARCHAR(30) NOT NULL,
    subject VARCHAR(200),
    notes TEXT,
    duration INT,
    logged_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_interactions_customer_id ON interactions(customer_id);
CREATE INDEX idx_interactions_logged_by ON interactions(logged_by);
CREATE INDEX idx_interactions_created_at ON interactions(created_at);
