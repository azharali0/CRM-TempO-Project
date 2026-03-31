CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(20),
    company VARCHAR(150),
    address TEXT,
    city VARCHAR(100),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    assigned_to UUID REFERENCES users(id),
    created_by UUID REFERENCES users(id),
    last_contacted_at TIMESTAMP,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_customers_assigned_to ON customers(assigned_to);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_email ON customers(email);
