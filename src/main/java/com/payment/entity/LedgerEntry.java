package com.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity mapping to the 'ledger_entries' table.
 *
 * THIS TABLE IS APPEND-ONLY. Once a row is written, it is NEVER modified.
 * This is enforced by:
 *   1. No UpdateTimestamp column (nothing to update).
 *   2. All @Column annotations have updatable = false.
 *   3. Application code in LedgerService never calls .save() on an
 *      existing LedgerEntry — only .save() on new ones.
 *
 * The ledger is the financial source of truth. Any wallet balance can be
 * reconstructed by summing all CREDIT entries minus all DEBIT entries
 * for a given account_id in this table.
 *
 * This is why financial systems keep a ledger even when they also have
 * a wallet balance — the ledger lets you audit and reconstruct.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_request_id", updatable = false, nullable = false)
    private UUID paymentRequestId;

    @Column(name = "account_id", updatable = false, nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", updatable = false, nullable = false, length = 10)
    private LedgerEntryType entryType;

    @Column(name = "amount", updatable = false, nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", updatable = false, nullable = false, length = 3)
    private String currency;

    @Column(name = "description", updatable = false, length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
