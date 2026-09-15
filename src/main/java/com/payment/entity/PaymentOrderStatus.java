package com.payment.entity;

/**
 * Status of a single PSP call attempt (one row in payment_orders).
 *
 * PENDING:   Order created, not yet sent to PSP.
 * SENT:      HTTP request dispatched to PSP, awaiting response.
 * SUCCEEDED: PSP confirmed the payment went through.
 * FAILED:    PSP rejected or timed out; may retry with another PSP.
 */
public enum PaymentOrderStatus {
    PENDING,
    SENT,
    SUCCEEDED,
    FAILED
}
