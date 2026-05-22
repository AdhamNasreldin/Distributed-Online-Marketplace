-- Shard 1 (shard_0) Schema & Seed Adaptation for MariaDB
CREATE DATABASE IF NOT EXISTS marketplace_shard1;
USE marketplace_shard1;

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

-- Seed Data (Shard 0 even users & products)
INSERT IGNORE INTO users (id, full_name, email, password_hash, balance, two_factor_enabled, created_at)
VALUES
  ('u-100', 'Youssef Adel', 'youssef@newera.local', 'plain:demo1234', 18650.00, TRUE, '2026-04-10 09:00:00'),
  ('u-300', 'Store Partner Cairo', 'partner@store.local', 'plain:demo1234', 9000.00, FALSE, '2026-04-18 17:15:00');

INSERT IGNORE INTO products (id, owner_id, name, brand, category, description, price, quantity, `condition`, status, listed_at, sold_count, color)
VALUES
  ('p-1004', 'u-100', 'Magic Keyboard', 'Apple', 'Accessories', 'Arabic-English layout keyboard for iPad Pro 11 inch.', 9800.00, 4, 'New', 'listed', '2026-04-29 08:10:00', 4, '#b45309'),
  ('p-1006', 'u-100', 'Ergo MX Mouse', 'Logitech', 'Accessories', 'Wireless ergonomic mouse with USB receiver and Bluetooth pairing.', 3600.00, 2, 'New', 'listed', '2026-05-05 12:26:00', 1, '#2563eb'),
  ('p-1002', 'u-300', 'ThinkPad X1 Carbon', 'Lenovo', 'Laptops', 'Business ultrabook, i7 processor, 16 GB RAM, 512 GB SSD, excellent battery.', 41000.00, 1, 'Used', 'listed', '2026-05-02 15:12:00', 3, '#334155'),
  ('p-1005', 'u-300', 'EOS M50 Mark II', 'Canon', 'Cameras', 'Mirrorless camera with kit lens, two batteries, and travel bag.', 27200.00, 1, 'Used', 'listed', '2026-05-04 19:05:00', 0, '#be123c');
