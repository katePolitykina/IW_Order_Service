package org.example.iw_order_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-ids")
    public ResponseEntity<List<OrderResponse>> getOrdersByIds(@RequestParam @Size(min = 1) List<@NotNull Long> orderIds)  {
        List<OrderResponse> responses = orderService.getOrdersByIds(orderIds);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatuses(@RequestParam @Size(min = 1)List<@NotNull OrderStatus> statuses) {
        List<OrderResponse> responses = orderService.getOrdersByStatuses(statuses);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@RequestBody @Valid UpdateOrderRequest request) {
        OrderResponse response = orderService.updateOrderStatus(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
