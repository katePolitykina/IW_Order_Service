package org.example.iw_order_service.service;

import org.example.iw_order_service.client.UserServiceClient;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.Item;
import org.example.iw_order_service.entity.Order;
import org.example.iw_order_service.entity.OrderItem;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.exception.InvalidStatusTransitionException;
import org.example.iw_order_service.exception.ItemNotFoundException;
import org.example.iw_order_service.exception.OrderNotFoundException;
import org.example.iw_order_service.mapper.OrderItemMapper;
import org.example.iw_order_service.mapper.OrderMapper;
import org.example.iw_order_service.repository.ItemRepository;
import org.example.iw_order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderService orderService;

    private UserResponse testUser;
    private Item testItem;
    private Order testOrder;
    private OrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        testUser = new UserResponse(1L, "John", "Doe", "test@example.com");
        testItem = new Item(1L, "Test Item",  BigDecimal.valueOf(100.00), null);
        testOrder = new Order(1L, 1L, OrderStatus.PENDING, LocalDateTime.now(), new ArrayList<>());
        testOrderResponse = new OrderResponse(1L, OrderStatus.PENDING, LocalDateTime.now(), new ArrayList<>(), testUser );
    }

    @Test
    void createOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest(1L, 2)));

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderItemMapper.toOrderItem(any(), any(), any())).thenReturn(new OrderItem());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toOrderResponse(testOrder, testUser)).thenReturn(testOrderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(testOrderResponse.getId(), result.getId());
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toOrderResponse(testOrder, testUser);
    }

    @Test
    void createOrder_ItemNotFound_ThrowsException() {
        CreateOrderRequest request =new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest(999L, 2)));

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(orderMapper.toOrderResponse(testOrder, testUser)).thenReturn(testOrderResponse);

        OrderResponse result = orderService.getOrder(1L);

        assertNotNull(result);
        assertEquals(testOrderResponse.getId(), result.getId());
        verify(orderRepository).findById(1L);
        verify(userServiceClient).getUserById(1L);
    }

    @Test
    void getOrder_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(999L));
        verify(userServiceClient, never()).getUserById(any());
    }

    @Test
    void getOrdersByIds_Success() {
        List<Long> orderIds = List.of(1L, 2L);
        Order order2 = new Order(2L, 2L, OrderStatus.PENDING, LocalDateTime.now(), new ArrayList<>());
        UserResponse user2 = new UserResponse(2L, "user2", "User", "user2@example.com");
        OrderResponse orderResponse2 = new OrderResponse(2L, OrderStatus.PENDING, LocalDateTime.now(), new ArrayList<>(), user2);


        when(orderRepository.findAllByIdIn(orderIds))
                .thenReturn(Stream.of(testOrder, order2));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.getUserById(2L)).thenReturn(user2);
        when(orderMapper.toOrderResponse(testOrder, testUser)).thenReturn(testOrderResponse);
        when(orderMapper.toOrderResponse(order2, user2)).thenReturn(orderResponse2);

        List<OrderResponse> result = orderService.getOrdersByIds(orderIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAllByIdIn(orderIds);
    }

    @Test
    void getOrdersByStatuses_Success() {
        List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.COMPLETED);

        Order confirmedOrder = new Order(2L, 1L, OrderStatus.COMPLETED, testOrder.getCreationDate(), new ArrayList<>());
        OrderResponse confirmedOrderResponse = new OrderResponse(2L, OrderStatus.COMPLETED, LocalDateTime.now(), new ArrayList<>(), testUser);

        when(orderRepository.findAllByStatusIn(statuses))
                .thenReturn(Stream.of(testOrder, confirmedOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(orderMapper.toOrderResponse(testOrder, testUser)).thenReturn(testOrderResponse);
        when(orderMapper.toOrderResponse(confirmedOrder, testUser)).thenReturn(confirmedOrderResponse);

        List<OrderResponse> result = orderService.getOrdersByStatuses(statuses);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAllByStatusIn(statuses);
    }


    @Test
    void updateOrderStatus_Success() {
        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setOrderId(1L);
        request.setStatus(OrderStatus.PENDING);

        Order updatedOrder = new Order(1L, 1L, OrderStatus.CANCELLED, LocalDateTime.now(), new ArrayList<>());
        OrderResponse updatedOrderResponse = new OrderResponse(1L, OrderStatus.CANCELLED, LocalDateTime.now(), new ArrayList<>(), testUser);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toOrderResponse(updatedOrder, testUser)).thenReturn(updatedOrderResponse);

        OrderResponse result = orderService.updateOrderStatus(request);

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order deliveredOrder = new Order(1L, 1L, OrderStatus.COMPLETED, LocalDateTime.now(), new ArrayList<>());
        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setOrderId(1L);
        request.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(deliveredOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);


        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateOrderStatus(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_OrderNotFound_ThrowsException() {
        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setOrderId(999L);
        request.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(request));
    }

    @Test
    void deleteOrder_Success_AsOwner() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ROLE_user"));
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(testOrder);
    }

    @Test
    void deleteOrder_Success_AsAdmin() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ROLE_iw.admin"));
        when(jwt.getClaimAsString("email")).thenReturn("admin@example.com");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(testOrder);
    }

    @Test
    void deleteOrder_AccessDenied_NotOwnerOrAdmin() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ROLE_user"));
        when(jwt.getClaimAsString("email")).thenReturn("other@example.com");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);

        assertThrows(AccessDeniedException.class, () -> orderService.deleteOrder(1L));
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void deleteOrder_IllegalState_OrderNotPending() {
        Order confirmedOrder = new Order(1L, 1L, OrderStatus.COMPLETED, LocalDateTime.now(), new ArrayList<>());

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(confirmedOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ROLE_user"));
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");

        assertThrows(IllegalStateException.class, () -> orderService.deleteOrder(1L));
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void deleteOrder_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(999L));
        verify(orderRepository, never()).delete(any());
    }
}