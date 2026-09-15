package com.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row per PSP call attempt for a given PaymentRequest.
 *
 * WHY SEPARATE FROM PaymentRequest?
 *   A payment may require multiple PSP attempts:
 *     Attempt 1 → Stripe FAILED (timeout)
 *     Attempt 2 → PayPal SUCCEEDED  ← circuit breaker switched PSPs
 *   PaymentRequest tracks the overall outcome.
 *   PaymentOrder tracks each individual attempt.
 *   This gives us full audit history + enables reconciliation.
 *
 * psp_reference is what we send TO the PSP. It's our idempotency key
 * for the PSP side — the same UUID on a retry tells the PSP "you already
 * saw this, return the existing result, don't charge again".
 */
@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_request_id", nullable = false)
    private UUID paymentRequestId;

    @Column(name = "psp_name", nullable = false, length = 50)
    private String pspName;

    /** UUID sent to the PSP — their idempotency key. */
    @Column(name = "psp_reference", nullable = false, updatable = false)
    private UUID pspReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    /** Raw JSON/text response from the PSP, stored for debugging. */
    @Column(name = "psp_response", columnDefinition = "TEXT")
    private String pspResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
