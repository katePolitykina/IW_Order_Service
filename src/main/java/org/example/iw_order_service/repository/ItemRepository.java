package org.example.iw_order_service.repository;

import org.example.iw_order_service.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
