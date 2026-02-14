package com.delivery.order.service;

import com.delivery.order.dto.OrderCreatedPayload;
import com.delivery.order.dto.OrderCreatedPayload.OrderItemDetail;
import com.delivery.order.dto.OrderItemRequest;
import com.delivery.order.dto.OrderRequest;
import com.delivery.order.entity.Order;
import com.delivery.order.entity.OrderItem;
import com.delivery.order.publisher.OrderEventPublisher;
import com.delivery.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for order creation.
 * 
 * CONCEPT: Service Layer
 * - Contains business logic (not in controller or repository)
 * - Coordinates between multiple components
 * - Handles transactions with @Transactional
 * 
 * Flow:
 * 1. Create Order entity from request
 * 2. Add items and calculate total
 * 3. Save to Postgres
 * 4. Publish order.created event to Kafka
 */
@Service
public class OrderService {

        private static final Logger log = LoggerFactory.getLogger(OrderService.class);

        private final OrderRepository orderRepository;
        private final OrderEventPublisher orderEventPublisher;

        public OrderService(OrderRepository orderRepository,
                        OrderEventPublisher orderEventPublisher) {
                this.orderRepository = orderRepository;
                this.orderEventPublisher = orderEventPublisher;
        }

        /**
         * Create a new order and publish the order.created event.
         * 
         * @param request The incoming order request from the REST endpoint
         * @return The created order
         */
        @Transactional
        public Order createOrder(OrderRequest request) {

                log.info("Creating order for customer: {}", request.getCustomerId());

                // Step 1: Create the order entity
                Order order = Order.create(request.getCustomerId(), request.getCurrency());

                // Step 2: Add items from the request
                for (OrderItemRequest itemReq : request.getItems()) {
                        OrderItem item = OrderItem.create(
                                        order,
                                        itemReq.getItemId(),
                                        itemReq.getQuantity(),
                                        itemReq.getUnitPrice());
                        order.addItem(item);
                }

                // Step 3: Save to database (cascade saves items too)
                order = orderRepository.save(order);

                log.info("Order saved: orderId={}, totalAmount={} {}",
                                order.getOrderId(), order.getTotalAmount(), order.getCurrency());

                // Step 4: Build event payload and publish
                List<OrderItemDetail> itemDetails = order.getItems().stream()
                                .map(item -> OrderItemDetail.from(
                                                item.getItemId(),
                                                item.getQuantity(),
                                                item.getUnitPrice()))
                                .collect(Collectors.toList());

                OrderCreatedPayload payload = OrderCreatedPayload.from(
                                order.getCustomerId(),
                                order.getTotalAmount(),
                                order.getCurrency(),
                                itemDetails);

                orderEventPublisher.publishOrderCreated(order.getOrderId(), payload);

                return order;
        }

        /**
         * Find an order by its business ID.
         * 
         * @param orderId The UUID string business identifier
         * @return The order, or empty if not found
         */
        public Order getOrder(String orderId) {
                return orderRepository.findByOrderId(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        }
}
