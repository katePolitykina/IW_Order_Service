package org.example.iw_order_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull
    @NotEmpty
    private List<OrderItemRequest> items;
}
