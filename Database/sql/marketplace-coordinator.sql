-- Core Coordinator Schema & Seed Adaptation for MariaDB
CREATE DATABASE IF NOT EXISTS marketplace_coordinator;
USE marketplace_coordinator;

CREATE TABLE IF NOT EXISTS auth_challenges (
  id VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  code VARCHAR(255) NOT NULL,
  purpose VARCHAR(50) NOT NULL CHECK (purpose IN ('register')),
  expires_at TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_challenges (
  id VARCHAR(255) PRIMARY KEY,
  buyer_id VARCHAR(255) NOT NULL,
  seller_id VARCHAR(255) NOT NULL,
  product_id VARCHAR(255) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  code VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchases (
  id VARCHAR(255) PRIMARY KEY,
  product_id VARCHAR(255) NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  buyer_id VARCHAR(255) NOT NULL,
  seller_id VARCHAR(255) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  purchased_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(50) NOT NULL CHECK (status IN ('completed', 'pending', 'failed'))
);

CREATE TABLE IF NOT EXISTS transactions (
  id VARCHAR(255) PRIMARY KEY,
  type VARCHAR(50) NOT NULL CHECK (type IN ('deposit', 'purchase', 'sale', 'refund')),
  amount DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
  from_user_id VARCHAR(255) NULL,
  to_user_id VARCHAR(255) NULL,
  product_id VARCHAR(255) NULL,
  description TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(50) NOT NULL CHECK (status IN ('completed', 'pending', 'failed'))
);

CREATE INDEX IF NOT EXISTS core_transactions_user_idx ON transactions(from_user_id, to_user_id);
CREATE INDEX IF NOT EXISTS core_transactions_product_idx ON transactions(product_id);
CREATE INDEX IF NOT EXISTS core_purchases_user_idx ON purchases(buyer_id, seller_id);

-- Seed Data
INSERT IGNORE INTO purchases (id, product_id, product_name, buyer_id, seller_id, amount, purchased_at, status)
VALUES
  ('ord-501', 'p-1004', 'Magic Keyboard', 'u-200', 'u-100', 9800.00, '2026-05-06 14:40:00', 'completed'),
  ('ord-502', 'p-1006', 'Ergo MX Mouse', 'u-300', 'u-100', 3600.00, '2026-05-07 09:05:00', 'completed');

INSERT IGNORE INTO transactions (id, type, amount, from_user_id, to_user_id, product_id, description, created_at, status)
VALUES
  ('tx-9001', 'deposit', 8000.00, NULL, 'u-100', NULL, 'Manual verified wallet deposit', '2026-05-03 09:00:00', 'completed'),
  ('tx-9002', 'sale', 9800.00, 'u-200', 'u-100', 'p-1004', 'Sold Magic Keyboard to Nour Hassan', '2026-05-06 14:40:00', 'completed'),
  ('tx-9003', 'sale', 3600.00, 'u-300', 'u-100', 'p-1006', 'Sold Ergo MX Mouse to Store Partner Cairo', '2026-05-07 09:05:00', 'completed');
