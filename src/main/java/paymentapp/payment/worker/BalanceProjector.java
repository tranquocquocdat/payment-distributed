package paymentapp.payment.worker;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.entity.LedgerEntry;
import paymentapp.payment.event.BalanceUpdateEvent;
import paymentapp.payment.repository.BalanceRepository;
import paymentapp.payment.repository.LedgerEntryRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceProjector {
    
    private final BalanceRepository balanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    
    @KafkaListener(topics = "ledger.hold_created", groupId = "balance-projector")
    @Transactional
    public void handleHoldCreated(BalanceUpdateEvent event) {
        try {
            // Verify the ledger entry exists with SUCCESS status
            if (ledgerEntryExists(event.getTxId(), event.getAccountId(), 
                    LedgerEntry.LegType.HOLD, LedgerEntry.TransactionStatus.SUCCESS)) {
                
                balanceRepository.createHold(event.getAccountId(), event.getAmount());
                log.info("Balance updated for HOLD_CREATED: account={}, amount={}", 
                        event.getAccountId(), event.getAmount());
            }
        } catch (Exception e) {
            log.error("Error processing HOLD_CREATED balance update", e);
        }
    }
    
    @KafkaListener(topics = "ledger.hold_released", groupId = "balance-projector")
    @Transactional
    public void handleHoldReleased(BalanceUpdateEvent event) {
        try {
            if (ledgerEntryExists(event.getTxId(), event.getAccountId(), 
                    LedgerEntry.LegType.RELEASE, LedgerEntry.TransactionStatus.SUCCESS)) {
                
                balanceRepository.releaseHold(event.getAccountId(), event.getAmount());
                log.info("Balance updated for HOLD_RELEASED: account={}, amount={}", 
                        event.getAccountId(), event.getAmount());
            }
        } catch (Exception e) {
            log.error("Error processing HOLD_RELEASED balance update", e);
        }
    }
    
    @KafkaListener(topics = "ledger.credit_posted", groupId = "balance-projector")
    @Transactional
    public void handleCreditPosted(BalanceUpdateEvent event) {
        try {
            if (ledgerEntryExists(event.getTxId(), event.getAccountId(), 
                    LedgerEntry.LegType.CREDIT, LedgerEntry.TransactionStatus.SUCCESS)) {
                
                balanceRepository.creditAmount(event.getAccountId(), event.getAmount());
                log.info("Balance updated for CREDIT_POSTED: account={}, amount={}", 
                        event.getAccountId(), event.getAmount());
            }
        } catch (Exception e) {
            log.error("Error processing CREDIT_POSTED balance update", e);
        }
    }
    
    @KafkaListener(topics = "ledger.debit_posted", groupId = "balance-projector")
    @Transactional
    public void handleDebitPosted(BalanceUpdateEvent event) {
        try {
            if (ledgerEntryExists(event.getTxId(), event.getAccountId(), 
                    LedgerEntry.LegType.DEBIT, LedgerEntry.TransactionStatus.SUCCESS)) {
                
                balanceRepository.debitAmount(event.getAccountId(), event.getAmount());
                log.info("Balance updated for DEBIT_POSTED: account={}, amount={}", 
                        event.getAccountId(), event.getAmount());
            }
        } catch (Exception e) {
            log.error("Error processing DEBIT_POSTED balance update", e);
        }
    }
    
    private boolean ledgerEntryExists(String txId, String accountId, 
            LedgerEntry.LegType legType, LedgerEntry.TransactionStatus status) {
        return ledgerEntryRepository.findByTxIdOrderByCreatedAt(txId)
                .stream()
                .anyMatch(entry -> entry.getAccountId().equals(accountId) 
                        && entry.getLegType() == legType 
                        && entry.getStatus() == status);
    }
}