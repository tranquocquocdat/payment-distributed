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
import paymentapp.payment.event.TransferCommittedEvent;
import paymentapp.payment.event.TransferCreditedEvent;
import paymentapp.payment.repository.BalanceRepository;
import paymentapp.payment.repository.LedgerEntryRepository;
import paymentapp.payment.repository.OutboxEventRepository;
import paymentapp.payment.repository.TransactionStatusRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitWorker {
    
    private final BalanceRepository balanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "transfer.credited", groupId = "commit-worker")
    @Transactional
    public void handleTransferCredited(TransferCreditedEvent event) {
        try {
            log.info("Processing COMMIT for txId: {}", event.getTxId());
            
            // Create debit entry
            LedgerEntry debitEntry = new LedgerEntry();
            debitEntry.setTxId(event.getTxId());
            debitEntry.setAccountId(event.getSourceAccount());
            debitEntry.setLegType(LedgerEntry.LegType.DEBIT);
            debitEntry.setAmount(event.getAmount());
            debitEntry.setStatus(LedgerEntry.TransactionStatus.SUCCESS);
            debitEntry.setDescription("Debit for transfer to " + event.getDestinationAccount());
            ledgerEntryRepository.save(debitEntry);
            
            // Create release entry
            LedgerEntry releaseEntry = new LedgerEntry();
            releaseEntry.setTxId(event.getTxId());
            releaseEntry.setAccountId(event.getSourceAccount());
            releaseEntry.setLegType(LedgerEntry.LegType.RELEASE);
            releaseEntry.setAmount(event.getAmount());
            releaseEntry.setStatus(LedgerEntry.TransactionStatus.SUCCESS);
            releaseEntry.setDescription("Release hold for transfer to " + event.getDestinationAccount());
            ledgerEntryRepository.save(releaseEntry);
            
            // Update transaction status
            transactionStatusRepository.findById(event.getTxId())
                .ifPresent(tx -> {
                    tx.setStatus(TransactionStatusEntity.Status.COMMITTED);
                    transactionStatusRepository.save(tx);
                });
            
            // Publish transfer.committed event
            TransferCommittedEvent committedEvent = new TransferCommittedEvent();
            committedEvent.setTxId(event.getTxId());
            committedEvent.setSourceAccount(event.getSourceAccount());
            committedEvent.setDestinationAccount(event.getDestinationAccount());
            committedEvent.setAmount(event.getAmount());
            committedEvent.setTimestamp(System.currentTimeMillis());
            
            publishEvent("transfer.committed", committedEvent, event.getSourceAccount());
            
            log.info("COMMIT successful for txId: {}, amount: {}", event.getTxId(), event.getAmount());
            
        } catch (Exception e) {
            log.error("Error processing COMMIT for txId: {}", event.getTxId(), e);
            handleCommitError(event, e.getMessage());
        }
    }
    
    private void handleCommitError(TransferCreditedEvent event, String errorMessage) {
        try {
            // Create failed debit entry
            LedgerEntry debitEntry = new LedgerEntry();
            debitEntry.setTxId(event.getTxId());
            debitEntry.setAccountId(event.getSourceAccount());
            debitEntry.setLegType(LedgerEntry.LegType.DEBIT);
            debitEntry.setAmount(event.getAmount());
            debitEntry.setStatus(LedgerEntry.TransactionStatus.FAILED);
            debitEntry.setDescription("Error during debit: " + errorMessage);
            ledgerEntryRepository.save(debitEntry);
            
            // Update transaction status
            transactionStatusRepository.findById(event.getTxId())
                .ifPresent(tx -> {
                    tx.setStatus(TransactionStatusEntity.Status.REJECTED);
                    tx.setErrorMessage(errorMessage);
                    transactionStatusRepository.save(tx);
                });
            
            // TODO: Implement compensation - reverse the credit
            
        } catch (Exception e) {
            log.error("Error handling commit error for txId: {}", event.getTxId(), e);
        }
    }
    
    private void publishEvent(String eventType, Object event, String partitionKey) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setTxId(((TransferCommittedEvent) event).getTxId());
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setPartitionKey(partitionKey);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Error publishing event: {}", eventType, e);
        }
    }
}