package paymentapp.payment.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.entity.LedgerEntry;
import paymentapp.payment.entity.OutboxEvent;
import paymentapp.payment.entity.TransactionStatusEntity;
import paymentapp.payment.event.TransferCancelledEvent;
import paymentapp.payment.repository.BalanceRepository;
import paymentapp.payment.repository.LedgerEntryRepository;
import paymentapp.payment.repository.OutboxEventRepository;
import paymentapp.payment.repository.TransactionStatusRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconcilerService {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${payment.reconciler.hold-timeout-minutes:5}")
    private int holdTimeoutMinutes;
    
    @Scheduled(fixedDelayString = "${payment.reconciler.schedule-interval:30000}")
    @Transactional
    public void reconcileOrphanedHolds() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(holdTimeoutMinutes);
            List<LedgerEntry> orphanedHolds = ledgerEntryRepository.findOrphanedHolds(cutoffTime);
            
            for (LedgerEntry hold : orphanedHolds) {
                log.warn("Found orphaned hold: txId={}, account={}, amount={}", 
                        hold.getTxId(), hold.getAccountId(), hold.getAmount());
                
                // Create release entry
                LedgerEntry releaseEntry = new LedgerEntry();
                releaseEntry.setTxId(hold.getTxId());
                releaseEntry.setAccountId(hold.getAccountId());
                releaseEntry.setLegType(LedgerEntry.LegType.RELEASE);
                releaseEntry.setAmount(hold.getAmount());
                releaseEntry.setStatus(LedgerEntry.TransactionStatus.FAILED);
                releaseEntry.setDescription("Auto-release orphaned hold due to timeout");
                ledgerEntryRepository.save(releaseEntry);
                
                // Release the hold in balance
                balanceRepository.releaseHold(hold.getAccountId(), hold.getAmount());
                
                // Update transaction status
                transactionStatusRepository.findById(hold.getTxId())
                    .ifPresent(tx -> {
                        tx.setStatus(TransactionStatusEntity.Status.CANCELLED);
                        tx.setErrorMessage("Transaction cancelled due to timeout");
                        transactionStatusRepository.save(tx);
                    });
                
                // Publish cancellation event
                TransferCancelledEvent cancelledEvent = new TransferCancelledEvent();
                cancelledEvent.setTxId(hold.getTxId());
                cancelledEvent.setSourceAccount(hold.getAccountId());
                cancelledEvent.setAmount(hold.getAmount());
                cancelledEvent.setReason("Transaction timeout");
                cancelledEvent.setTimestamp(System.currentTimeMillis());
                
                OutboxEvent outboxEvent = new OutboxEvent();
                outboxEvent.setTxId(hold.getTxId());
                outboxEvent.setEventType("transfer.cancelled");
                outboxEvent.setPayload(objectMapper.writeValueAsString(cancelledEvent));
                outboxEvent.setPartitionKey(hold.getAccountId());
                outboxEventRepository.save(outboxEvent);
                
                log.info("Reconciled orphaned hold: txId={}", hold.getTxId());
            }
            
            if (!orphanedHolds.isEmpty()) {
                log.info("Reconciled {} orphaned holds", orphanedHolds.size());
            }
            
        } catch (Exception e) {
            log.error("Error during reconciliation", e);
        }
    }
}