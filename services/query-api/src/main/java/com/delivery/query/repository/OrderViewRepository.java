package com.delivery.query.repository;

import com.delivery.query.entity.OrderView;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderViewRepository extends CassandraRepository<OrderView, String> {
}
