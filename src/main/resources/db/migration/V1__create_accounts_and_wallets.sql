-- =============================================================================
-- V1: Accounts and Wallets
-- =============================================================================
-- WHY TWO TABLES?
--   An "Account" is the identity (who you are — email, name).
--   A "Wallet" is the financial state (how much money you have).
--   Separating them means we can add multiple wallets per account later
--   (e.g., different currencies) without touching the accounts table.
-- =============================================================================

-- ===== Accounts Table =====
-- Represents a registered user or business on the platform.
CREATE TABLE accounts (
    -- UUID primary key: globally unique, safe for distributed systems,
    -- and doesn't leak sequential IDs to clients.
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- The user's external reference (email). UNIQUE + NOT NULL enforced at DB level.
    email         VARCHAR(255) NOT NULL,

    -- Human-readable display name.
    full_name     VARCHAR(255) NOT NULL,

    -- Soft-delete flag: we never physically DELETE accounts (audit trail).
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit timestamps — set automatically, never updated by application code.
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_email UNIQUE (email)
);

-- Index for lookup by email (login / account resolution).
CREATE INDEX idx_accounts_email ON accounts (email);


-- ===== Wallets Table =====
-- One wallet per account (for now). Tracks the spendable balance.
CREATE TABLE wallets (
    id            UUID           NOT NULL DEFAULT gen_random_uuid(),

    -- Foreign key back to the owning account.
    -- ON DELETE RESTRICT: you cannot delete an account if it has a wallet (data safety).
    account_id    UUID           NOT NULL,

    -- Balance stored as NUMERIC(19, 4):
    --   19 digits total, 4 decimal places.
    --   NEVER use FLOAT or DOUBLE for money — IEEE-754 floating point cannot
    --   represent 0.10 exactly, leading to rounding errors in financial calculations.
    --   In Java we will always use BigDecimal to match.
    balance       NUMERIC(19,4)  NOT NULL DEFAULT 0.0000,

    -- ISO 4217 currency code (e.g. 'USD', 'EUR', 'INR').
    currency      VARCHAR(3)     NOT NULL DEFAULT 'USD',

    -- Optimistic locking version column.
    -- JPA @Version uses this to detect concurrent modifications:
    -- if two threads try to update the same wallet at the same time,
    -- one will fail with OptimisticLockException instead of silently overwriting.
    version       BIGINT         NOT NULL DEFAULT 0,

    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_wallets PRIMARY KEY (id),
    -- Each account has exactly one wallet per currency.
    CONSTRAINT uq_wallets_account_currency UNIQUE (account_id, currency),
    CONSTRAINT fk_wallets_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    -- Balance can never go negative at DB level (last line of defence).
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_account_id ON wallets (account_id);
