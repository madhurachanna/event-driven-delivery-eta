package com.delivery.order.repository;

import com.delivery.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Order entity.
 * 
 * CONCEPT: Spring Data JPA Repository
 * - Extend JpaRepository<Entity, IdType> to get CRUD operations for free
 * - Spring auto-implements this interface at runtime
 * - Method names become queries: findByOrderId → SELECT * WHERE order_id = ?
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by its business identifier (UUID string).
     * Spring generates: SELECT * FROM orders WHERE order_id = ?
     */
    Optional<Order> findByOrderId(String orderId);
}
