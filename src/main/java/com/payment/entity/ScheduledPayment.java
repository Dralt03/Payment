package com.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity mapping to the 'scheduled_payments' table.
 *
 * Represents a user-configured recurring payment rule.
 * The PaymentSchedulerRunner queries this table every minute,
 * finds rows where: is_active = true AND next_run_at <= NOW()
 * then fires a payment and advances next_run_at to the following interval.
 *
 * Soft-delete pattern: DELETE requests set is_active=false.
 * We never physically delete the row — we keep history.
 *
 * cron_expression: standard 5-field cron string.
 *   "0 9 1 * *"  → 9:00 AM on the 1st of every month
 *   "0 0 * * 1"  → midnight every Monday
 *   We use Spring's CronExpression class to compute next_run_at after each fire.
 */
@Entity
@Table(name = "scheduled_payments")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option", nullable = false, length = 50)
    private PaymentOption paymentOption;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    /** Scheduler polls for rows where next_run_at <= NOW(). Updated after each fire. */
    @Column(name = "next_run_at", nullable = false)
    private OffsetDateTime nextRunAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
