package com.payment.entity;

/**
 * The aggregate status of a PaymentRequest across its full lifecycle.
 *
 * State machine:
 *   PENDING → PROCESSING → SUCCEEDED
 *                        → FAILED
 *
 * PENDING:    Request received and validated, not yet sent to PSP.
 * PROCESSING: At least one PaymentOrder has been sent to a PSP.
 * SUCCEEDED:  PSP confirmed success; Ledger and Wallet updated.
 * FAILED:     All PSP attempts failed; no money moved.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
