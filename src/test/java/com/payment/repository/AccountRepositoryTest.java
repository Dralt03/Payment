package com.payment.repository;

import com.payment.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindAccount() {
        Account account = new Account();
        account.setFullName("Test Account");
        account.setEmail("test@example.com");

        Account saved = entityManager.persistAndFlush(account);
        entityManager.clear(); // Clear L1 cache to force DB hit on find

        assertThat(saved.getId()).isNotNull();
        
        Account found = accountRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getFullName()).isEqualTo("Test Account");
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getCreatedAt()).isNotNull(); // Automatically set by @CreationTimestamp
        assertThat(found.getUpdatedAt()).isNotNull(); // Automatically set by @UpdateTimestamp
    }
}
