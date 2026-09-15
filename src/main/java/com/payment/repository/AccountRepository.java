package com.payment.repository;

import com.payment.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Account.
 *
 * JpaRepository<Account, UUID> gives us for free:
 *   save(account)       → INSERT or UPDATE
 *   findById(id)        → SELECT by PK
 *   findAll()           → SELECT *
 *   delete(account)     → DELETE (we won't use this — we soft-delete)
 *   existsById(id)      → SELECT COUNT
 *   ...and more
 *
 * We add findByEmail — Spring parses this method name and generates:
 *   SELECT * FROM accounts WHERE email = ?
 * No SQL needed. If the method name is wrong, it fails at startup (not runtime).
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
}
