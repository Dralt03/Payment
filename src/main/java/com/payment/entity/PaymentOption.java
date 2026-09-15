package com.payment.entity;

/**
 * Payment method the user selected at checkout.
 * Determines which PSP integration the Payment Processing Service will use.
 */
public enum PaymentOption {
    BANK,
    CREDIT_CARD,
    PAYPAL,
    APPLE_PAY,
    GOOGLE_PAY
}
