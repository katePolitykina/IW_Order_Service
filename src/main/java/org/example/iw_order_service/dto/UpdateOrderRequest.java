package org.example.iw_order_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.iw_order_service.entity.enums.OrderStatus;

@Data
public class UpdateOrderRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private OrderStatus status;
}
