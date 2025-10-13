package org.example.iw_order_service.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.iw_order_service.client.UserServiceClient;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.Item;
import org.example.iw_order_service.entity.OrderItem;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.exception.ForbiddenException;
import org.example.iw_order_service.exception.InvalidStatusTransitionException;
import org.example.iw_order_service.exception.ItemNotFoundException;
import org.example.iw_order_service.exception.OrderNotFoundException;
import org.example.iw_order_service.mapper.OrderItemMapper;
import org.example.iw_order_service.mapper.OrderMapper;
import org.example.iw_order_service.repository.ItemRepository;
import org.example.iw_order_service.repository.OrderRepository;
import org.example.iw_order_service.security.SecurityService;
import org.example.iw_order_service.service.kafka.OrderEventProducer;
import org.springframework.stereotype.Service;

import org.example.iw_order_service.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final SecurityService securityService;
    private final OrderEventProducer orderEventProducer;
    public OrderResponse createOrder(CreateOrderRequest request) {
        UserResponse userInfo = userServiceClient.getUserById(securityService.getCurrentUserId());
        Order order = new Order();
        order.setUserId(userInfo.getId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemRequest.getItemId()));
            OrderItem orderItem =orderItemMapper.toOrderItem(itemRequest, order, item);
            order.getOrderItems().add(orderItem);

            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        order.setStatus(OrderStatus.PENDING);
        order.setCreationDate(LocalDateTime.now());
        order = orderRepository.save(order);

        PaymentRequest paymentRequest = new PaymentRequest(order.getId(), order.getUserId(), totalAmount);
        orderEventProducer.sendCreateOrderEvent(paymentRequest);

        return orderMapper.toOrderResponse(order,userInfo);
    }

    public OrderResponse getOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
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

    public OrderResponse updateOrderStatus(UpdateOrderRequest request) {
        if (!securityService.hasRole("ROLE_iw.admin")) {
            throw new ForbiddenException("Only admins can update order status");
        }
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
    public void updateOrderStatusInternal (UpdateOrderRequest request){
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found or access denied: " + request.getOrderId()));
        order.setStatus(request.getStatus());
        orderRepository.save(order);
    }

    public void deleteOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or access denied: " + orderId));
        UserResponse userInfo = userServiceClient.getUserById(order.getUserId());

        boolean isAdmin = securityService.hasRole("ROLE_iw.admin");
        boolean isOwner = securityService.getCurrentUserId().equals(userInfo.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Only admins can update order status");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot delete order with status: " + order.getStatus());
        }
        orderRepository.delete(order);
    }

    public List<OrderResponse> getAllOrders() {
        var userId = securityService.getCurrentUserId();
        UserResponse userInfo = userServiceClient.getUserById(userId);
        return orderRepository.findAllByUserId(userId)
                .map(order -> orderMapper.toOrderResponse(order, userInfo))
                .collect(Collectors.toList());
    }
}
