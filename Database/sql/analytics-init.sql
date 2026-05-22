-- ANALYTICS INIT — Flattened / Dimensional Tracking
CREATE DATABASE IF NOT EXISTS marketplace_analytics;
USE marketplace_analytics;

-- A combined dimension table for products and categories to optimize analytical queries
CREATE TABLE IF NOT EXISTS dim_products (
    sku VARCHAR(100),
    product_name VARCHAR(200),
    category_name VARCHAR(100),
    parent_category_name VARCHAR(100),
    price DECIMAL(10,2),
    weight DECIMAL(8,2)
);

-- Snapshot table to capture stock trends over time
CREATE TABLE IF NOT EXISTS fact_inventory_snapshots (
    snapshot_date DATE DEFAULT (CURRENT_DATE),
    sku VARCHAR(100),
    warehouse_id INT,
    warehouse_name VARCHAR(150),
    quantity_on_hand INT
);
