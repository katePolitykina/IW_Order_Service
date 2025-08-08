package org.example.iw_order_service.repository;

import org.example.iw_order_service.entity.Order;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Stream<Order> findAllByIdIn(List<Long> ids);
    Stream<Order> findAllByStatusIn(List<OrderStatus> statuses);


}
