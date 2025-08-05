package org.example.iw_order_service.dto;

import lombok.Data;
import org.example.iw_order_service.entity.enums.OrderStatus;

@Data
public class UpdateOrderRequest {
    private Long orderId;
    private OrderStatus status;
}
