package org.example.iw_order_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.service.OrderService;
import org.springframework.http.HttpStatus;
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
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);

    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/by-ids")
    public List<OrderResponse> getOrdersByIds(@RequestParam @Size(min = 1) List<@NotNull Long> orderIds)  {
        return orderService.getOrdersByIds(orderIds);
    }

    @GetMapping("/by-status")
    public List<OrderResponse> getOrdersByStatuses(@RequestParam @Size(min = 1)List<@NotNull OrderStatus> statuses) {
        return orderService.getOrdersByStatuses(statuses);

    }

    @PutMapping("/status")
    public OrderResponse updateOrderStatus(@RequestBody @Valid UpdateOrderRequest request) {
        return orderService.updateOrderStatus(request);
    }

    @DeleteMapping("/{orderId}")
    public void deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
    }

}
