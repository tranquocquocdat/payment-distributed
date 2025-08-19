-- Database initialization script
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE payment_db;

-- Create user
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_user'@'%';
FLUSH PRIVILEGES;

-- Account master table
CREATE TABLE account_mst (
    account_id VARCHAR(20) PRIMARY KEY,
    account_name VARCHAR(100) NOT NULL,
    account_type ENUM('CHECKING', 'SAVINGS', 'BUSINESS') NOT NULL,
    status ENUM('ACTIVE', 'SUSPENDED', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Balances table
CREATE TABLE balances (
    account_id VARCHAR(20) PRIMARY KEY,
    book DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    open_hold DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_book_positive CHECK (book >= 0),
    CONSTRAINT chk_available_positive CHECK (available >= 0),
    CONSTRAINT chk_open_hold_positive CHECK (open_hold >= 0),
    INDEX idx_updated_at (updated_at),
    FOREIGN KEY (account_id) REFERENCES account_mst(account_id)
);

-- Ledger entries table
CREATE TABLE ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_id VARCHAR(50) NOT NULL,
    account_id VARCHAR(20) NOT NULL,
    leg_type ENUM('HOLD', 'RELEASE', 'DEBIT', 'CREDIT') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_tx_account_leg (tx_id, account_id, leg_type),
    INDEX idx_account_created (account_id, created_at),
    INDEX idx_tx_id (tx_id),
    INDEX idx_status (status),
    INDEX idx_leg_type_status (leg_type, status),
    FOREIGN KEY (account_id) REFERENCES account_mst(account_id)
);

-- Outbox events table
CREATE TABLE outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    partition_key VARCHAR(50) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,

    INDEX idx_processed_created (processed, created_at),
    INDEX idx_tx_id (tx_id),
    INDEX idx_partition_key (partition_key)
);

-- Idempotency keys table
CREATE TABLE idempotency_keys (
    tx_id VARCHAR(50) PRIMARY KEY,
    request_hash VARCHAR(64),
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    response JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,

    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);

-- Transaction status table
CREATE TABLE transaction_status (
    tx_id VARCHAR(50) PRIMARY KEY,
    source_account VARCHAR(20) NOT NULL,
    destination_account VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('REQUESTED', 'HELD', 'CREDITED', 'COMMITTED', 'REJECTED', 'CANCELLED') NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_source_account (source_account),
    INDEX idx_destination_account (destination_account),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (source_account) REFERENCES account_mst(account_id),
    FOREIGN KEY (destination_account) REFERENCES account_mst(account_id)
);

-- Sample data
INSERT INTO account_mst (account_id, account_name, account_type, status) VALUES
('ACC001', 'Nguyen Van A', 'CHECKING', 'ACTIVE'),
('ACC002', 'Le Van B', 'CHECKING', 'ACTIVE'),
('ACC003', 'Tran Thi C', 'SAVINGS', 'ACTIVE'),
('ACC004', 'Pham Van D', 'BUSINESS', 'ACTIVE');

INSERT INTO balances (account_id, book, available, open_hold) VALUES
('ACC001', 300000.00, 300000.00, 0.00),
('ACC002', 0.00, 0.00, 0.00),
('ACC003', 500000.00, 500000.00, 0.00),
('ACC004', 1000000.00, 1000000.00, 0.00);