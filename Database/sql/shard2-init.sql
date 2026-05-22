-- SHARD 2 INIT — Users with odd user_id (1,3,5..)

CREATE DATABASE IF NOT EXISTS marketplace_shard2;
USE marketplace_shard2;

-- Same tables as shard1, just different database 
CREATE TABLE IF NOT EXISTS `User` (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    registration_date DATETIME DEFAULT NOW(),
    account_status ENUM('active','banned','suspended') DEFAULT 'active',
    INDEX idx_username (username),
    INDEX idx_email (email)
) PARTITION BY HASH(user_id) PARTITIONS 2;

CREATE TABLE IF NOT EXISTS `Admin` (
    admin_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    admin_level INT DEFAULT 1,
    PRIMARY KEY (admin_id, user_id),
    INDEX idx_user_id (user_id)
) PARTITION BY HASH(user_id) PARTITIONS 2;

CREATE TABLE IF NOT EXISTS `Seller` (
    seller_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    seller_status ENUM('active','inactive','suspended') DEFAULT 'active',
    approved_at DATETIME,
    PRIMARY KEY (seller_id, user_id),
    INDEX idx_user_id (user_id)
) PARTITION BY HASH(user_id) PARTITIONS 2;

CREATE TABLE IF NOT EXISTS `DeliveryPerson` (
    delivery_person_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    availability_status ENUM('available','busy','offline') DEFAULT 'available',
    PRIMARY KEY (delivery_person_id, user_id),
    INDEX idx_user_id (user_id)
) PARTITION BY HASH(user_id) PARTITIONS 2;

CREATE TABLE IF NOT EXISTS `Wallet` (
    wallet_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT NOW(),
    PRIMARY KEY (wallet_id, user_id),
    INDEX idx_user_id (user_id)
) PARTITION BY HASH(user_id) PARTITIONS 2;


-- ======== Adham's part ========

CREATE TABLE IF NOT EXISTS `Cart` (

    cart_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,

    created_at DATETIME DEFAULT NOW(),

    updated_at DATETIME DEFAULT NOW()
        ON UPDATE NOW(),

    PRIMARY KEY (cart_id, user_id),

    INDEX idx_user_id (user_id)

)
PARTITION BY HASH(user_id)
PARTITIONS 2;



CREATE TABLE IF NOT EXISTS `Orders` (

    order_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,

    placed_at DATETIME DEFAULT NOW(),

    status ENUM(
        'pending',
        'confirmed',
        'shipped',
        'delivered',
        'cancelled'
    ) DEFAULT 'pending',

    total_amount DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (order_id, user_id),

    INDEX idx_user_id (user_id)

)
PARTITION BY HASH(user_id)
PARTITIONS 2;



CREATE TABLE IF NOT EXISTS `OrderItem` (

    order_item_id INT NOT NULL AUTO_INCREMENT,

    order_id INT NOT NULL,

    listing_id INT NOT NULL,

    quantity INT NOT NULL,

    unit_price DECIMAL(10,2) NOT NULL,

    line_total DECIMAL(10,2) NOT NULL,

    updated_at DATETIME DEFAULT NOW()
        ON UPDATE NOW(),

    PRIMARY KEY (order_item_id, order_id),

    INDEX idx_order_id (order_id),
    INDEX idx_listing_id (listing_id)

)
PARTITION BY HASH(order_id)
PARTITIONS 2;



CREATE TABLE IF NOT EXISTS `Transactions` (

    transaction_id INT NOT NULL AUTO_INCREMENT,

    wallet_id INT NOT NULL,

    order_id INT,

    transaction_date DATETIME DEFAULT NOW(),

    status ENUM(
        'pending',
        'completed',
        'failed',
        'refunded'
    ) DEFAULT 'pending',

    type ENUM(
        'purchase',
        'deposit',
        'refund',
        'withdrawal'
    ),

    amount DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (transaction_id, transaction_date),

    INDEX idx_wallet_id (wallet_id),
    INDEX idx_order_id (order_id)

)

PARTITION BY RANGE (YEAR(transaction_date)) (

    PARTITION p2025 VALUES LESS THAN (2026),

    PARTITION p2026 VALUES LESS THAN (2027),

    PARTITION p_future VALUES LESS THAN MAXVALUE
);
-- =========================================================================
--  REPLICATED TABLES (Shard 2)
-- =========================================================================

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
