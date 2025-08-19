package paymentapp.payment.worker;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.entity.LedgerEntry;
import paymentapp.payment.entity.OutboxEvent;
import paymentapp.payment.entity.TransactionStatusEntity;
import paymentapp.payment.event.TransferHeldEvent;
import paymentapp.payment.event.TransferRejectedEvent;
import paymentapp.payment.event.TransferRequestedEvent;
import paymentapp.payment.repository.BalanceRepository;
import paymentapp.payment.repository.LedgerEntryRepository;
import paymentapp.payment.repository.OutboxEventRepository;
import paymentapp.payment.repository.TransactionStatusRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldWorker {
    
    private final BalanceRepository balanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "transfer.requested", groupId = "hold-worker")
    @Transactional
    public void handleTransferRequested(TransferRequestedEvent event) {
        try {
            log.info("Processing HOLD for txId: {}", event.getTxId());
            
            // Try to create hold using CAS (Compare-And-Swap)
            int holdCreated = balanceRepository.createHold(
                event.getSourceAccount(), 
                event.getAmount()
            );
            
            LedgerEntry holdEntry = new LedgerEntry();
            holdEntry.setTxId(event.getTxId());
            holdEntry.setAccountId(event.getSourceAccount());
            holdEntry.setLegType(LedgerEntry.LegType.HOLD);
            holdEntry.setAmount(event.getAmount());
            holdEntry.setDescription("Hold for transfer to " + event.getDestinationAccount());
            
            if (holdCreated > 0) {
                // Successfully created hold
                holdEntry.setStatus(LedgerEntry.TransactionStatus.SUCCESS);
                ledgerEntryRepository.save(holdEntry);
                
                // Update transaction status
                transactionStatusRepository.findById(event.getTxId())
                    .ifPresent(tx -> {
                        tx.setStatus(TransactionStatusEntity.Status.HELD);
                        transactionStatusRepository.save(tx);
                    });
                
                // Publish transfer.held event
                TransferHeldEvent heldEvent = new TransferHeldEvent();
                heldEvent.setTxId(event.getTxId());
                heldEvent.setSourceAccount(event.getSourceAccount());
                heldEvent.setDestinationAccount(event.getDestinationAccount());
                heldEvent.setAmount(event.getAmount());
                heldEvent.setTimestamp(System.currentTimeMillis());
                
                publishEvent("transfer.held", heldEvent, event.getSourceAccount());
                
                log.info("HOLD successful for txId: {}, amount: {}", event.getTxId(), event.getAmount());
                
            } else {
                // Insufficient funds
                holdEntry.setStatus(LedgerEntry.TransactionStatus.FAILED);
                holdEntry.setDescription("Insufficient funds for transfer");
                ledgerEntryRepository.save(holdEntry);
                
                // Update transaction status
                transactionStatusRepository.findById(event.getTxId())
                    .ifPresent(tx -> {
                        tx.setStatus(TransactionStatusEntity.Status.REJECTED);
                        tx.setErrorMessage("Insufficient funds");
                        transactionStatusRepository.save(tx);
                    });
                
                // Publish transfer.rejected event
                TransferRejectedEvent rejectedEvent = new TransferRejectedEvent();
                rejectedEvent.setTxId(event.getTxId());
                rejectedEvent.setSourceAccount(event.getSourceAccount());
                rejectedEvent.setDestinationAccount(event.getDestinationAccount());
                rejectedEvent.setAmount(event.getAmount());
                rejectedEvent.setReason("Insufficient funds");
                rejectedEvent.setTimestamp(System.currentTimeMillis());
                
                publishEvent("transfer.rejected", rejectedEvent, event.getSourceAccount());
                
                log.warn("HOLD failed for txId: {} - Insufficient funds", event.getTxId());
            }
            
        } catch (Exception e) {
            log.error("Error processing HOLD for txId: {}", event.getTxId(), e);
            handleHoldError(event, e.getMessage());
        }
    }
    
    private void handleHoldError(TransferRequestedEvent event, String errorMessage) {
        try {
            // Create failed hold entry
            LedgerEntry holdEntry = new LedgerEntry();
            holdEntry.setTxId(event.getTxId());
            holdEntry.setAccountId(event.getSourceAccount());
            holdEntry.setLegType(LedgerEntry.LegType.HOLD);
            holdEntry.setAmount(event.getAmount());
            holdEntry.setStatus(LedgerEntry.TransactionStatus.FAILED);
            holdEntry.setDescription("Error during hold: " + errorMessage);
            ledgerEntryRepository.save(holdEntry);
            
            // Update transaction status
            transactionStatusRepository.findById(event.getTxId())
                .ifPresent(tx -> {
                    tx.setStatus(TransactionStatusEntity.Status.REJECTED);
                    tx.setErrorMessage(errorMessage);
                    transactionStatusRepository.save(tx);
                });
            
            // Publish rejection event
            TransferRejectedEvent rejectedEvent = new TransferRejectedEvent();
            rejectedEvent.setTxId(event.getTxId());
            rejectedEvent.setSourceAccount(event.getSourceAccount());
            rejectedEvent.setDestinationAccount(event.getDestinationAccount());
            rejectedEvent.setAmount(event.getAmount());
            rejectedEvent.setReason(errorMessage);
            rejectedEvent.setTimestamp(System.currentTimeMillis());
            
            publishEvent("transfer.rejected", rejectedEvent, event.getSourceAccount());
            
        } catch (Exception e) {
            log.error("Error handling hold error for txId: {}", event.getTxId(), e);
        }
    }
    
    private void publishEvent(String eventType, Object event, String partitionKey) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setTxId(getEventTxId(event));
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setPartitionKey(partitionKey);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Error publishing event: {}", eventType, e);
        }
    }
    
    private String getEventTxId(Object event) {
        if (event instanceof TransferHeldEvent) {
            return ((TransferHeldEvent) event).getTxId();
        } else if (event instanceof TransferRejectedEvent) {
            return ((TransferRejectedEvent) event).getTxId();
        }
        return "unknown";
    }
}