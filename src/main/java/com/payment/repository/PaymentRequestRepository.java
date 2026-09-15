package com.payment.repository;

import com.payment.entity.PaymentRequest;
import com.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    /** Used for idempotency check — has this checkout UUID been processed before? */
    Optional<PaymentRequest> findByCheckoutUuid(UUID checkoutUuid);

    boolean existsByCheckoutUuid(UUID checkoutUuid);

    List<PaymentRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentRequest> findByStatus(PaymentStatus status);
}
