-- SHARD 1 INIT — Users with even user_id (0,2,4..)

CREATE DATABASE IF NOT EXISTS marketplace_shard1;
USE marketplace_shard1;

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