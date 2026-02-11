package com.delivery.payment.repository;

import com.delivery.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Payment entity.
 * 
 * CONCEPT: Spring Data JPA Repository
 * - Extend JpaRepository<Entity, IdType> to get CRUD operations for free
 * - Spring auto-implements this interface at runtime
 * - Method names become queries: findByOrderId → SELECT * WHERE order_id = ?
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID.
     * Spring generates: SELECT * FROM payments WHERE order_id = ?
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Check if payment already exists for this idempotency key.
     * Used to detect duplicates before insert attempt.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
