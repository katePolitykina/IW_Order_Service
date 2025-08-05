package org.example.iw_order_service.mapper;

import org.example.iw_order_service.dto.OrderResponse;
import org.example.iw_order_service.dto.UserResponse;
import org.example.iw_order_service.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {
    @Mapping(source = "orderItems", target = "items")
    OrderResponse toOrderResponse(Order order, UserResponse userInfo);
}
