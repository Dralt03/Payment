-- =============================================================================
-- V2: Payments, Ledger Entries, and Scheduled Payments
-- =============================================================================
-- PAYMENT FLOW OVERVIEW:
--   1. Client submits a payment → creates a "payment_requests" row (intent)
--   2. We call the PSP → creates a "payment_orders" row per PSP attempt
--   3. On success → creates two "ledger_entries" rows (double-entry)
--   4. On success → updates both wallets
-- =============================================================================

-- ===== Payment Requests Table =====
-- Represents the CLIENT'S intent to pay. Created once per checkout action.
-- This is where idempotency lives: checkout_uuid is UNIQUE so the same
-- "pay" button click can never create two payment requests.
CREATE TABLE payment_requests (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- checkout_uuid comes from the frontend. It's generated when the checkout
    -- page loads, so even if the user hammers the "Pay" button, we only
    -- process it once. This is the IDEMPOTENCY KEY.
    checkout_uuid   UUID         NOT NULL,

    -- Who is paying, and between which accounts.
    user_id         UUID         NOT NULL,
    from_account_id UUID         NOT NULL,
    to_account_id   UUID         NOT NULL,

    -- Amount as NUMERIC — same reason as wallets: no floating point money.
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'USD',

    -- Which PSP option the user selected: BANK, CREDIT_CARD, PAYPAL, APPLE_PAY
    payment_option  VARCHAR(50)  NOT NULL,

    -- Aggregate status of the whole payment lifecycle.
    -- Values: PENDING → PROCESSING → SUCCEEDED | FAILED
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment_requests PRIMARY KEY (id),
    -- DB-level idempotency guarantee — even if app-level check fails, DB rejects duplicates.
    CONSTRAINT uq_payment_requests_checkout_uuid UNIQUE (checkout_uuid),
    CONSTRAINT fk_payment_requests_user    FOREIGN KEY (user_id)         REFERENCES accounts(id),
    CONSTRAINT fk_payment_requests_from    FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_payment_requests_to      FOREIGN KEY (to_account_id)   REFERENCES accounts(id),
    CONSTRAINT chk_payment_requests_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_requests_status CHECK (status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED'))
);

CREATE INDEX idx_payment_requests_user_id      ON payment_requests (user_id);
CREATE INDEX idx_payment_requests_status       ON payment_requests (status);
CREATE INDEX idx_payment_requests_created_at   ON payment_requests (created_at DESC);


-- ===== Payment Orders Table =====
-- One row per PSP call attempt. A single payment_request may have
-- MULTIPLE payment_orders if the first PSP attempt fails and we retry
-- with a different PSP (circuit-breaker pattern).
CREATE TABLE payment_orders (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- Links back to the original payment intent.
    payment_request_id  UUID         NOT NULL,

    -- Which PSP was called for this attempt (e.g. 'STRIPE', 'PAYPAL', 'MOCK').
    psp_name            VARCHAR(50)  NOT NULL,

    -- The UUID we send TO the PSP. The PSP uses this for its own idempotency.
    -- If our request is retried, we send the same psp_reference so the PSP
    -- knows it already processed it and won't charge twice.
    psp_reference       UUID         NOT NULL,

    -- Status of THIS PSP attempt (not the overall payment).
    -- Values: PENDING → SENT → SUCCEEDED | FAILED
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    -- Raw response from the PSP — stored for debugging and reconciliation.
    psp_response        TEXT,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment_orders PRIMARY KEY (id),
    CONSTRAINT uq_payment_orders_psp_reference UNIQUE (psp_reference),
    CONSTRAINT fk_payment_orders_request FOREIGN KEY (payment_request_id) REFERENCES payment_requests(id),
    CONSTRAINT chk_payment_orders_status CHECK (status IN ('PENDING','SENT','SUCCEEDED','FAILED'))
);

CREATE INDEX idx_payment_orders_request_id ON payment_orders (payment_request_id);
CREATE INDEX idx_payment_orders_status     ON payment_orders (status);


-- ===== Ledger Entries Table =====
-- The immutable financial record. Uses double-entry accounting:
--   Every payment creates EXACTLY TWO rows:
--     Row 1: DEBIT  from_account  (money leaves)
--     Row 2: CREDIT to_account    (money arrives)
--   The sum of all ledger entries for a given account = its true balance.
--   This table is APPEND-ONLY — no UPDATE or DELETE ever happens here.
--   This is the source of truth for all financial history.
CREATE TABLE ledger_entries (
    id                  UUID           NOT NULL DEFAULT gen_random_uuid(),

    -- Which payment created these entries.
    payment_request_id  UUID           NOT NULL,

    -- The account this entry affects.
    account_id          UUID           NOT NULL,

    -- DEBIT = money going OUT, CREDIT = money coming IN.
    entry_type          VARCHAR(10)    NOT NULL,

    -- Always positive. The sign of the effect is captured by entry_type.
    amount              NUMERIC(19,4)  NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'USD',

    -- Human-readable description for statements.
    description         VARCHAR(500),

    -- IMMUTABLE timestamp — when this financial fact was recorded.
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entries_request FOREIGN KEY (payment_request_id) REFERENCES payment_requests(id),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id)          REFERENCES accounts(id),
    CONSTRAINT chk_ledger_entries_type   CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_entries_amount CHECK (amount > 0)
    -- NOTE: No updated_at column — this row is never updated.
);

CREATE INDEX idx_ledger_entries_account_id   ON ledger_entries (account_id);
CREATE INDEX idx_ledger_entries_request_id   ON ledger_entries (payment_request_id);
CREATE INDEX idx_ledger_entries_created_at   ON ledger_entries (created_at DESC);


-- ===== Scheduled Payments Table =====
-- Stores the configuration for recurring/future payments.
-- The PaymentSchedulerRunner polls this table every minute,
-- finds rows where next_run_at <= NOW() and is_active = true,
-- then submits them as regular payment_requests.
CREATE TABLE scheduled_payments (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),

    -- The account that configured this schedule.
    user_id             UUID          NOT NULL,
    from_account_id     UUID          NOT NULL,
    to_account_id       UUID          NOT NULL,

    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    payment_option      VARCHAR(50)   NOT NULL,

    -- Cron expression string, e.g. '0 9 1 * *' = 9am on the 1st of every month.
    cron_expression     VARCHAR(100)  NOT NULL,

    -- When should the next execution fire? Updated after each run.
    next_run_at         TIMESTAMPTZ   NOT NULL,

    -- Soft-delete: DELETE request marks is_active=false, doesn't remove the row.
    -- This preserves history of what was scheduled.
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_scheduled_payments PRIMARY KEY (id),
    CONSTRAINT fk_scheduled_payments_user  FOREIGN KEY (user_id)         REFERENCES accounts(id),
    CONSTRAINT fk_scheduled_payments_from  FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_scheduled_payments_to    FOREIGN KEY (to_account_id)   REFERENCES accounts(id),
    CONSTRAINT chk_scheduled_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_scheduled_payments_next_run ON scheduled_payments (next_run_at) WHERE is_active = TRUE;
CREATE INDEX idx_scheduled_payments_user_id  ON scheduled_payments (user_id);
