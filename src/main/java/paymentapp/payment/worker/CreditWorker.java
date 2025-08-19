package paymentapp.payment.worker;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.entity.Account;
import paymentapp.payment.entity.LedgerEntry;
import paymentapp.payment.entity.OutboxEvent;
import paymentapp.payment.entity.TransactionStatusEntity;
import paymentapp.payment.event.TransferCreditedEvent;
import paymentapp.payment.event.TransferHeldEvent;
import paymentapp.payment.event.TransferRejectedEvent;
import paymentapp.payment.repository.AccountRepository;
import paymentapp.payment.repository.LedgerEntryRepository;
import paymentapp.payment.repository.OutboxEventRepository;
import paymentapp.payment.repository.TransactionStatusRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditWorker {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "transfer.held", groupId = "credit-worker")
    @Transactional
    public void handleTransferHeld(TransferHeldEvent event) {
        try {
            log.info("Processing CREDIT for txId: {}", event.getTxId());
            
            // Validate destination account
            if (accountRepository.findByAccountIdAndStatus(
                    event.getDestinationAccount(), 
                    Account.AccountStatus.ACTIVE).isEmpty()) {
                
                handleCreditError(event, "Destination account not found or inactive");
                return;
            }
            
            // Create credit entry
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTxId(event.getTxId());
            creditEntry.setAccountId(event.getDestinationAccount());
            creditEntry.setLegType(LedgerEntry.LegType.CREDIT);
            creditEntry.setAmount(event.getAmount());
            creditEntry.setStatus(LedgerEntry.TransactionStatus.SUCCESS);
            creditEntry.setDescription("Credit from transfer from " + event.getSourceAccount());
            ledgerEntryRepository.save(creditEntry);
            
            // Update transaction status
            transactionStatusRepository.findById(event.getTxId())
                .ifPresent(tx -> {
                    tx.setStatus(TransactionStatusEntity.Status.CREDITED);
                    transactionStatusRepository.save(tx);
                });
            
            // Publish transfer.credited event
            TransferCreditedEvent creditedEvent = new TransferCreditedEvent();
            creditedEvent.setTxId(event.getTxId());
            creditedEvent.setSourceAccount(event.getSourceAccount());
            creditedEvent.setDestinationAccount(event.getDestinationAccount());
            creditedEvent.setAmount(event.getAmount());
            creditedEvent.setTimestamp(System.currentTimeMillis());
            
            publishEvent("transfer.credited", creditedEvent, event.getSourceAccount());
            
            log.info("CREDIT successful for txId: {}, amount: {}", event.getTxId(), event.getAmount());
            
        } catch (Exception e) {
            log.error("Error processing CREDIT for txId: {}", event.getTxId(), e);
            handleCreditError(event, e.getMessage());
        }
    }
    
    private void handleCreditError(TransferHeldEvent event, String errorMessage) {
        try {
            // Create failed credit entry
            LedgerEntry creditEntry = new LedgerEntry();
            creditEntry.setTxId(event.getTxId());
            creditEntry.setAccountId(event.getDestinationAccount());
            creditEntry.setLegType(LedgerEntry.LegType.CREDIT);
            creditEntry.setAmount(event.getAmount());
            creditEntry.setStatus(LedgerEntry.TransactionStatus.FAILED);
            creditEntry.setDescription("Error during credit: " + errorMessage);
            ledgerEntryRepository.save(creditEntry);
            
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
            log.error("Error handling credit error for txId: {}", event.getTxId(), e);
        }
    }
    
    private void publishEvent(String eventType, Object event, String partitionKey) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setTxId(((TransferCreditedEvent) event).getTxId());
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setPartitionKey(partitionKey);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Error publishing event: {}", eventType, e);
        }
    }
}