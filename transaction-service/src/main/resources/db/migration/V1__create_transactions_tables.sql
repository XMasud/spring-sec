-- ==========================================
-- 1. Table: transactions
-- Source of truth for financial balances
-- ==========================================
CREATE TABLE transactions
(
    id                UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    user_id           UUID                     NOT NULL,
    source_account_id VARCHAR(64)              NOT NULL,
    target_account_id VARCHAR(64)              NOT NULL,
    idempotency_key   VARCHAR(64)              NOT NULL UNIQUE,
    amount            NUMERIC(18, 4)           NOT NULL,
    currency          VARCHAR(3)               NOT NULL,
    status            VARCHAR(32)              NOT NULL DEFAULT 'PENDING',
    risk_score        NUMERIC(3, 2)            NULL,
    version           INTEGER                  NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_status ON transactions (status);

-- ==========================================
-- 2. Table: transaction_metadata
-- Contextual data required by Fraud Engine
-- ==========================================
CREATE TABLE transaction_metadata
(
    transaction_id     UUID PRIMARY KEY REFERENCES transactions (id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(256)  NOT NULL,
    ip_address         INET          NOT NULL,
    latitude           NUMERIC(9, 6) NULL,
    longitude          NUMERIC(9, 6) NULL,
    payment_channel    VARCHAR(32)   NOT NULL
);

-- ==========================================
-- 3. Table: transactional_outbox
-- Transactional Outbox Pattern for Kafka
-- ==========================================
CREATE TABLE transactional_outbox
(
    id            UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    aggregate_id  VARCHAR(64)              NOT NULL,
    event_type    VARCHAR(64)              NOT NULL,
    partition_key VARCHAR(64)              NOT NULL,
    payload       JSONB                    NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed     BOOLEAN                  NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unprocessed ON transactional_outbox (processed, created_at) WHERE processed = FALSE;

-- ==========================================
-- Trigger to automatically update updated_at
-- ==========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE
    ON transactions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
