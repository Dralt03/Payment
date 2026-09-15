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
 * JPA Entity mapping to the 'wallets' table.
 *
 * KEY DESIGN POINTS:
 *
 * 1. BigDecimal for money
 *    Java's BigDecimal maps to SQL NUMERIC exactly — no precision loss.
 *    The @Column precision=19, scale=4 must match the migration exactly.
 *
 * 2. @Version for optimistic locking
 *    When WalletService reads a wallet and then updates it, two concurrent
 *    requests could read the same balance (e.g. $100) and both try to deduct $80.
 *    Without locking, both would succeed, leaving the account at -$60.
 *    With @Version, the second update sees the version has changed and throws
 *    OptimisticLockException — we catch this and retry.
 *    This is much lighter than pessimistic (SELECT FOR UPDATE) locking.
 *
 * 3. No @OneToOne back to Account
 *    We intentionally don't add Account account field here.
 *    Relationships like @OneToOne can cause N+1 query problems if not
 *    carefully managed. We join via account_id when needed in queries.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /**
     * precision=19 → total digits, scale=4 → digits after decimal.
     * e.g. 9_999_999_999_999_999.9999 is the max storable value.
     * Always initialise to ZERO — never null.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /**
     * @Version tells JPA to use optimistic locking on this entity.
     * Hibernate automatically increments this on every UPDATE.
     * If two transactions read version=5, the first to commit sets version=6.
     * The second will see version=5 in the WHERE clause no longer matches
     * and throw OptimisticLockException.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Wallet(UUID accountId, String currency) {
        this.accountId = accountId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
    }
}
