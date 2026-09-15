package com.payment.repository;

import com.payment.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {

    List<ScheduledPayment> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * The core polling query for the scheduler.
     * Finds all active scheduled payments whose next_run_at time has passed.
     * The scheduler calls this every minute and fires each result as a payment.
     */
    @Query("SELECT s FROM ScheduledPayment s WHERE s.isActive = true AND s.nextRunAt <= :now")
    List<ScheduledPayment> findDuePayments(OffsetDateTime now);
}
