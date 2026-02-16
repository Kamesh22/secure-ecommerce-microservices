-- Initialize Product Database
CREATE SCHEMA IF NOT EXISTS public;

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (price > 0)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_product_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_product_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_product_created_at ON products(created_at DESC);

-- Sample data in Indian Rupees
INSERT INTO products (name, description, price)
VALUES 
    ('Laptop', 'High performance laptop for professionals', 95000.00),
    ('Smartphone', 'Latest model smartphone with 5G', 65000.00),
    ('Tablet', 'Portable tablet for work and entertainment', 40000.00),
    ('Headphones', 'Wireless noise-canceling headphones', 15000.00),
    ('Charger', 'Universal USB-C charger', 1500.00)
ON CONFLICT DO NOTHING;