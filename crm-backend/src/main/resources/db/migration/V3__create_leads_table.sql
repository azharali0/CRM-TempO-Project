CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    title VARCHAR(200) NOT NULL,
    stage VARCHAR(30) DEFAULT 'NEW',
    value DECIMAL(12,2),
    expected_close_date DATE,
    probability INT DEFAULT 0,
    lost_reason TEXT,
    owner_id UUID REFERENCES users(id),
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_leads_owner_id ON leads(owner_id);
CREATE INDEX idx_leads_stage ON leads(stage);
CREATE INDEX idx_leads_customer_id ON leads(customer_id);
