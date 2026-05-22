-- COORDINATOR INIT — Reference Schema for Replicated Node Routing
CREATE DATABASE IF NOT EXISTS marketplace_coordinator;
USE marketplace_coordinator;

CREATE TABLE IF NOT EXISTS `Category` (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INT,
    FOREIGN KEY (parent_id) REFERENCES Category(category_id)
);

CREATE TABLE IF NOT EXISTS `Product` (
    sku VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    weight DECIMAL(8,2),
    images JSON,
    category_id INT,
    FOREIGN KEY (category_id) REFERENCES Category(category_id)
);

CREATE TABLE IF NOT EXISTS `ProductListing` (
    listing_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    available_quantity INT DEFAULT 0,
    listing_status ENUM('active','inactive','sold_out') DEFAULT 'active',
    created_at DATETIME DEFAULT NOW(),
    FOREIGN KEY (sku) REFERENCES Product(sku)
);

CREATE TABLE IF NOT EXISTS `ProductSubmission` (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    submitted_id INT NOT NULL,
    reviewed_id INT,
    proposed_name VARCHAR(200),
    proposed_sku VARCHAR(100),
    status ENUM('pending','approved','rejected') DEFAULT 'pending',
    review_note TEXT
);

CREATE TABLE IF NOT EXISTS `Warehouse` (
    warehouse_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS `Shipment` (
    shipment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    warehouse_id INT,
    tracking_number VARCHAR(100),
    status ENUM('pending','shipped','delivered','returned') DEFAULT 'pending',
    shipped_at DATETIME,
    delivered_at DATETIME,
    FOREIGN KEY (warehouse_id) REFERENCES Warehouse(warehouse_id)
);

CREATE TABLE IF NOT EXISTS `Stock` (
    listing_id INT NOT NULL,
    warehouse_id INT NOT NULL,
    quantity INT DEFAULT 0,
    PRIMARY KEY (listing_id, warehouse_id),
    FOREIGN KEY (listing_id) REFERENCES ProductListing(listing_id),
    FOREIGN KEY (warehouse_id) REFERENCES Warehouse(warehouse_id)
);
