package com.delivery.delivery.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false, unique = true)
    private String deliveryId;

    @Column(nullable = false)
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private Instant estimatedDeliveryTime;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Delivery() {
    }

    public static Delivery create(String orderId, String driverName,
            Instant estimatedDeliveryTime) {
        Delivery d = new Delivery();
        d.orderId = orderId;
        d.deliveryId = "DEL-" + UUID.randomUUID().toString().substring(0, 8);
        d.driverName = driverName;
        d.status = DeliveryStatus.ASSIGNED;
        d.estimatedDeliveryTime = estimatedDeliveryTime;
        d.createdAt = Instant.now();
        return d;
    }

    public void updateEta(Instant newEta) {
        this.estimatedDeliveryTime = newEta;
        this.updatedAt = Instant.now();
    }

    public void markInTransit() {
        this.status = DeliveryStatus.IN_TRANSIT;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getDriverName() {
        return driverName;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
