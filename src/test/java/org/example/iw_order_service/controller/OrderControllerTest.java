package org.example.iw_order_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import jakarta.transaction.Transactional;
import org.example.iw_order_service.IwOrderServiceApplication;
import org.example.iw_order_service.dto.*;
import org.example.iw_order_service.entity.Item;
import org.example.iw_order_service.entity.Order;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.repository.ItemRepository;
import org.example.iw_order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {IwOrderServiceApplication.class})
@WireMockTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class OrderControllerTest {
    private UserResponse testUser;
    private Item testItem;
    private Order testOrder;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired
    private OrderRepository orderRepository;
    
    private final String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testItem = new Item(null, "Test Item",  BigDecimal.valueOf(100.00), null);
        testItem = itemRepository.save(testItem);

        testUser = new UserResponse(1L, "John", "Doe", userEmail);

        testOrder= new Order(null, testUser.getId(), OrderStatus.PENDING, LocalDateTime.now(),null );
        testOrder= orderRepository.save(testOrder);

    }



    @RegisterExtension
    static WireMockExtension userServiceWM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicPort())
            .build();

    @DynamicPropertySource
    public static void setUpMockBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("service.userservice.url", userServiceWM::baseUrl);

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    public void createOrder_Success() throws Exception {
        stubUserByEmail();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest(testItem.getId(), 2)));

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.user")))
                .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemId").value(testItem.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.userInfo").exists());

    }

    @Test
    public void createOrder_UserException() throws Exception {
        userServiceWM.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/api/internal/v1.0/users/internal"))
                        .withQueryParam("email", WireMock.equalTo(testUser.getEmail()))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.NOT_FOUND.value())));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("User service returned error")))
        ;

    }
    @Test
    void createOrder_InvalidRequest_BadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of());

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void createOrder_ItemNotFound_NotFound() throws Exception {

        stubUserByEmail();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest(999L, 2)));

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("Item not found")));
    }

    @Test
    void getOrder_Success() throws Exception {
        stubUserById();
        mockMvc.perform(get("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.userInfo.id").value(testUser.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
    @Test
    void getOrder_NotFound() throws Exception {
        stubUserById();
        mockMvc.perform(get("/api/v1/orders/{orderId}", 999L)
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByIds_Success() throws Exception {
        Order order1 = new Order(null, testUser.getId(), OrderStatus.PENDING, LocalDateTime.now(),null );
        order1 = orderRepository.save(order1);

        stubUserById();

        mockMvc.perform(get("/api/v1/orders/by-ids")
                        .param("orderIds", order1.getId().toString(), testOrder.getId().toString())
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    @Test
    void getOrdersByIds_EmptyList_BadRequest() throws Exception {
        stubUserById();
        mockMvc.perform(get("/api/v1/orders/by-ids")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                        .authorities(new SimpleGrantedAuthority("ROLE_iw.admin"))))
                .andExpect(status().isBadRequest());
    }


    @Test
    void getOrdersByStatuses_Success() throws Exception {

        Order order2 = new Order(null, testUser.getId(), OrderStatus.CANCELLED, LocalDateTime.now(),null );
        orderRepository.save(order2);
        stubUserById();

        mockMvc.perform(get("/api/v1/orders/by-status")
                        .param("statuses", "PENDING", "COMPLETED")
                        .with(jwt().jwt(builder -> builder
                                .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(testOrder.getId()));
    }

    @Test
    void updateOrderStatus_Success() throws Exception {
        UpdateOrderRequest request =new  UpdateOrderRequest();
        request.setOrderId(testOrder.getId());
        request.setStatus(OrderStatus.COMPLETED);

        stubUserById();
        Jwt jwt = Jwt.withTokenValue("token-value")
                .claim("email", "admin@example.com")
                .claim("roles", List.of("iw.admin"))
                .header("alg", "none")
                .build();

        System.out.println("jwt = " + jwt.getClaims());

        mockMvc.perform(put("/api/v1/orders/status")
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", "admin@example.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateOrderStatus_Unauthorized_Forbidden() throws Exception {

        UpdateOrderRequest request =new  UpdateOrderRequest();
        request.setOrderId(testOrder.getId());
        request.setStatus(OrderStatus.COMPLETED);

        stubUserById();

        mockMvc.perform(put("/api/v1/orders/status")
                        .with(jwt().jwt(builder -> builder
                                .claim("email", userEmail)
                                .claim("roles", List.of("ROLE_iw.user"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrder_Success_AsOwner() throws Exception {
        stubUserById();
        mockMvc.perform(delete("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", testUser.getEmail()))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.user"))))
                .andExpect(status().isOk());


        mockMvc.perform(get("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                        .claim("email", userEmail))
                                .authorities(new SimpleGrantedAuthority("ROLE_iw.user"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_Success_AsAdmin() throws Exception {
        stubUserById();
        mockMvc.perform(delete("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                .claim("email", "admin@example.com")
                                .claim("roles", List.of("ROLE_iw.admin")))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteOrder_AccessDenied_Forbidden() throws Exception {
        stubUserById();
        mockMvc.perform(delete("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                .claim("email", "other@example.com")
                                .claim("roles", List.of("ROLE_iw.user")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrder_OrderNotPending_Conflict() throws Exception {
        stubUserById();
        testOrder.setStatus(OrderStatus.COMPLETED);
        mockMvc.perform(delete("/api/v1/orders/{orderId}", testOrder.getId())
                        .with(jwt().jwt(builder -> builder
                                .claim("email", userEmail)
                                .claim("roles", List.of("ROLE_iw.user")))))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOrder_NotFound() throws Exception {
        stubUserById();
        mockMvc.perform(delete("/api/v1/orders/{orderId}", 999L)
                        .with(jwt().jwt(builder -> builder
                                .claim("email", userEmail)
                                .claim("roles", List.of("ROLE_iw.user")))))
                .andExpect(status().isNotFound());
    }


    private void stubUserByEmail () throws JsonProcessingException {
        userServiceWM.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/api/internal/v1.0/users/internal"))
                        .withQueryParam("email", WireMock.equalTo(testUser.getEmail()))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(testUser)))
        );
    }
    private void stubUserById() throws JsonProcessingException {
        userServiceWM.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/api/internal/v1.0/users/internal"))
                        .withQueryParam("id", WireMock.equalTo(testUser.getId().toString()))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(testUser)))
        );
    }

}
