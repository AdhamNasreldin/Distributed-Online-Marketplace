-- Shard 2 (shard_1) Schema & Seed Adaptation for MariaDB
CREATE DATABASE IF NOT EXISTS marketplace_shard2;
USE marketplace_shard2;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(255) PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
  two_factor_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(255) PRIMARY KEY,
  owner_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(255) NOT NULL,
  category VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  price DECIMAL(12, 2) NOT NULL CHECK (price > 0),
  quantity INT NOT NULL CHECK (quantity >= 0),
  `condition` VARCHAR(50) NOT NULL CHECK (`condition` IN ('New', 'Like New', 'Used')),
  status VARCHAR(50) NOT NULL CHECK (status IN ('listed', 'sold', 'draft')),
  listed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sold_count INT NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
  color VARCHAR(50) NOT NULL DEFAULT '#0f766e'
);

CREATE INDEX IF NOT EXISTS products_owner_idx ON products(owner_id);

-- Seed Data (Shard 1 odd users & products)
INSERT IGNORE INTO users (id, full_name, email, password_hash, balance, two_factor_enabled, created_at)
VALUES
  ('u-200', 'Nour Hassan', 'nour@newera.local', 'plain:demo1234', 4300.00, TRUE, '2026-04-12 13:30:00');

INSERT IGNORE INTO products (id, owner_id, name, brand, category, description, price, quantity, `condition`, status, listed_at, sold_count, color)
VALUES
  ('p-1001', 'u-200', 'Galaxy Tab S9', 'Samsung', 'Tablets', 'AMOLED Android tablet with keyboard cover, S Pen, and original charger.', 22500.00, 2, 'Like New', 'listed', '2026-05-01 10:20:00', 1, '#0f766e'),
  ('p-1003', 'u-200', 'WH-1000XM5 Headphones', 'Sony', 'Audio', 'Noise cancelling headphones with carrying case and Type-C cable.', 14200.00, 3, 'Like New', 'listed', '2026-05-03 11:48:00', 2, '#7c3aed');
