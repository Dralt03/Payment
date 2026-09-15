package com.payment.repository;

import com.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByAccountIdAndCurrency(UUID accountId, String currency);

    /**
     * Pessimistic write lock — used when we KNOW we're about to update the balance.
     *
     * WHY USE THIS alongside @Version (optimistic lock)?
     *   @Version is great for low-contention cases. But for wallet debits/credits
     *   under high concurrency, we use a SELECT FOR UPDATE to acquire a DB-level
     *   row lock immediately. This prevents the retry loop that optimistic locking
     *   would require. The trade-off: slightly lower throughput, but zero retries.
     *   In a payments context, correctness > throughput.
     */
    @Query("SELECT w FROM Wallet w WHERE w.accountId = :accountId AND w.currency = :currency")
    Optional<Wallet> findByAccountIdAndCurrencyForUpdate(UUID accountId, String currency);
}
