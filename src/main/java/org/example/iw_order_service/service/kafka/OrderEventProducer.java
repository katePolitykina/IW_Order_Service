package org.example.iw_order_service.service.kafka;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.iw_order_service.dto.PaymentRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@AllArgsConstructor
public class OrderEventProducer {
    private static final String TOPIC = "create-order-topic";

    private final KafkaTemplate<String, PaymentRequest> kafkaTemplate;


    public void sendCreateOrderEvent(PaymentRequest event) {
        log.info("Sending CREATE_ORDER event: {}", event);

        CompletableFuture<SendResult<String, PaymentRequest>> future =
                kafkaTemplate.send(TOPIC, Long.toString(event.getOrderId()), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent CREATE_ORDER event for orderId: {} with offset: {}",
                        event.getOrderId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send CREATE_ORDER event for orderId: {}",
                        event.getOrderId(), ex);
            }
        });
    }
}
