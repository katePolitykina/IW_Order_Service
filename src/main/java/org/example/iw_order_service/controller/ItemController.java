package org.example.iw_order_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.iw_order_service.dto.ItemResponse;
import org.example.iw_order_service.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    @GetMapping
    public Page<ItemResponse> getAll(Pageable pageable) {
        return itemService.getAll(pageable);
    }
    @GetMapping("/{itemId}")
    public ItemResponse getItem(@PathVariable Long itemId) {
        return itemService.getById(itemId);
    }

}
