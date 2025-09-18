package org.example.iw_order_service.service.kafka;


import lombok.AllArgsConstructor;
import org.example.iw_order_service.dto.PaymentResponse;
import org.example.iw_order_service.dto.UpdateOrderRequest;
import org.example.iw_order_service.dto.enums.PaymentStatus;
import org.example.iw_order_service.entity.enums.OrderStatus;
import org.example.iw_order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;


    @KafkaListener(topics = "create-payment-topic", groupId = "order-service-group")
    public void handleCreatePaymentEvent(
            @Payload PaymentResponse paymentResponse,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("Received CREATE_PAYMENT event from topic: {}, partition: {}, offset: {}, event: {}",
                topic, partition, offset, paymentResponse);

        try {
            UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest();
            if (paymentResponse.getStatus() == PaymentStatus.SUCCESS) {
                updateOrderRequest.setStatus(OrderStatus.PAYED);
            } else {
                updateOrderRequest.setStatus(OrderStatus.CANCELLED);
            }
            updateOrderRequest.setOrderId(paymentResponse.getOrderId());

            orderService.updateOrderStatusInternal(updateOrderRequest);

            acknowledgment.acknowledge();
            logger.info("Successfully processed CREATE_PAYMENT event for orderId: {}", paymentResponse.getOrderId());

        } catch (Exception ex) {
            logger.error("Failed to process CREATE_PAYMENT event for orderId: {}", paymentResponse.getOrderId(), ex);
        }

    }


}

