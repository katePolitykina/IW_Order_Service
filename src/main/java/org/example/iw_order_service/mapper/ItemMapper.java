package org.example.iw_order_service.mapper;

import org.example.iw_order_service.dto.ItemResponse;
import org.example.iw_order_service.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    ItemResponse toItemResponse( Item item);

}
