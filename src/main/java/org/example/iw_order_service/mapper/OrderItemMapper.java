package org.example.iw_order_service.mapper;


import org.example.iw_order_service.dto.OrderItemRequest;
import org.example.iw_order_service.dto.OrderItemResponse;
import org.example.iw_order_service.entity.Item;
import org.example.iw_order_service.entity.Order;
import org.example.iw_order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    OrderItem toOrderItem(OrderItemRequest request, Order order, Item item);

    @Mapping(source = "item.id", target = "itemId")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

}
