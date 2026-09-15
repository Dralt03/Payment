package com.payment.repository;

import com.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<LedgerEntry> findByPaymentRequestId(UUID paymentRequestId);

    /**
     * Calculates the true balance from the ledger for an account.
     * CREDIT entries add to balance, DEBIT entries subtract.
     * This can be used to verify the wallet balance is correct (reconciliation).
     *
     * COALESCE handles the case where there are no entries yet (returns 0 instead of null).
     */
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END),
            0
        )
        FROM LedgerEntry e
        WHERE e.accountId = :accountId AND e.currency = :currency
        """)
    BigDecimal calculateBalanceFromLedger(UUID accountId, String currency);
}
