package org.example.iw_order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.iw_order_service.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private LocalDateTime creationDate;
    private List<OrderItemResponse> items;
    private UserResponse userInfo;
}
