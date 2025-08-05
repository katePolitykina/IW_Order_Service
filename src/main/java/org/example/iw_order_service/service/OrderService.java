package org.example.iw_order_service.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.iw_order_service.client.UserServiceClient;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.Item;
import org.example.iw_order_service.entity.OrderItem;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.exception.InvalidStatusTransitionException;
import org.example.iw_order_service.exception.ItemNotFoundException;
import org.example.iw_order_service.exception.OrderNotFoundException;
import org.example.iw_order_service.mapper.OrderItemMapper;
import org.example.iw_order_service.mapper.OrderMapper;
import org.example.iw_order_service.repository.ItemRepository;
import org.example.iw_order_service.repository.OrderRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import org.example.iw_order_service.entity.Order;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserServiceClient userServiceClient;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    public OrderResponse createOrder(CreateOrderRequest request) {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userEmail = jwt.getClaimAsString("email");
        UserResponse userInfo = userServiceClient.getUserByEmail(userEmail);
        Order order = new Order();
        order.setUserId(userInfo.getId());
        for (OrderItemRequest itemRequest : request.getItems()) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemRequest.getItemId()));
            OrderItem orderItem =orderItemMapper.toOrderItem(itemRequest, order, item);
            order.getOrderItems().add(orderItem);
        }
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);
        return orderMapper.toOrderResponse(order,userInfo);
    }

    public OrderResponse getOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or access denied: " + orderId));
        UserResponse userInfo = userServiceClient.getUserById(order.getUserId());
        return orderMapper.toOrderResponse(order, userInfo);
    }
    public List<OrderResponse> getOrdersByIds(List<Long> ids) {

        return orderRepository.findAllByIdIn(ids)
                .map(order -> {
                    UserResponse userInfo = userServiceClient.getUserById(order.getUserId());
                    return orderMapper.toOrderResponse(order, userInfo);
                })
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatuses(List<OrderStatus> status) {

        return orderRepository.findAllByStatusIn(status)
                .map(order -> {
                    UserResponse userInfo = userServiceClient.getUserById(order.getUserId());
                    return orderMapper.toOrderResponse(order, userInfo);
                })
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('iw.admin')")
    public OrderResponse updateOrderStatus(UpdateOrderRequest request) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found or access denied: " + request.getOrderId()));

        UserResponse userInfo = userServiceClient.getUserById(order.getUserId());
        OrderStatus newStatus = request.getStatus();

        if (order.getStatus().ordinal()> newStatus.ordinal()) {
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + order.getStatus() + " to " +newStatus);
        }
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        return orderMapper.toOrderResponse(order,userInfo);
    }

    public void deleteOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or access denied: " + orderId));
        UserResponse userInfo = userServiceClient.getUserById(order.getUserId());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        var roles = jwt.getClaimAsStringList("roles");
        boolean isAdmin = roles != null && roles.contains("ROLE_iw.admin");
        boolean isOwner = jwt.getClaimAsString("email").equals(userInfo.getEmail());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to delete this order.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot delete order with status: " + order.getStatus());
        }
        orderRepository.delete(order);
    }

}
