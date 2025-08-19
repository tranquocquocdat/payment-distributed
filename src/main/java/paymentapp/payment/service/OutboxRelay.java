package paymentapp.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.entity.OutboxEvent;
import paymentapp.payment.event.*;
import paymentapp.payment.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Scheduled(fixedDelay = 1000) // Every 1 second
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> unprocessedEvents = outboxEventRepository
                .findByProcessedFalseOrderByCreatedAt();
            
            for (OutboxEvent event : unprocessedEvents) {
                try {
                    // Convert JSON payload to appropriate event object
                    Object eventObject = convertPayloadToEvent(event.getEventType(), event.getPayload());
                    
                    // Send to Kafka
                    kafkaTemplate.send(event.getEventType(), event.getPartitionKey(), eventObject)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                // Mark as processed
                                outboxEventRepository.markAsProcessed(event.getId(), LocalDateTime.now());
                                log.debug("Published event: type={}, txId={}", 
                                        event.getEventType(), event.getTxId());
                            } else {
                                log.error("Failed to publish event: type={}, txId={}", 
                                        event.getEventType(), event.getTxId(), ex);
                            }
                        });
                        
                } catch (Exception e) {
                    log.error("Error processing outbox event: id={}", event.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in outbox relay", e);
        }
    }
    
    private Object convertPayloadToEvent(String eventType, String payload) throws Exception {
        return switch (eventType) {
            case "transfer.requested" -> objectMapper.readValue(payload, TransferRequestedEvent.class);
            case "transfer.held" -> objectMapper.readValue(payload, TransferHeldEvent.class);
            case "transfer.credited" -> objectMapper.readValue(payload, TransferCreditedEvent.class);
            case "transfer.committed" -> objectMapper.readValue(payload, TransferCommittedEvent.class);
            case "transfer.rejected" -> objectMapper.readValue(payload, TransferRejectedEvent.class);
            case "transfer.cancelled" -> objectMapper.readValue(payload, TransferCancelledEvent.class);
            default -> objectMapper.readValue(payload, Object.class);
        };
    }
}