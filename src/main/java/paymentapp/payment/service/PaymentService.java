package paymentapp.payment.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paymentapp.payment.dto.AccountBalanceResponse;
import paymentapp.payment.dto.TransactionStatusResponse;
import paymentapp.payment.dto.TransferRequest;
import paymentapp.payment.dto.TransferResponse;
import paymentapp.payment.entity.Account;
import paymentapp.payment.entity.IdempotencyKey;
import paymentapp.payment.entity.OutboxEvent;
import paymentapp.payment.entity.TransactionStatusEntity;
import paymentapp.payment.event.TransferRequestedEvent;
import paymentapp.payment.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request) {
        try {
            // Check idempotency
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
                .findByTxIdAndExpiresAtAfter(request.getIdempotencyKey(), LocalDateTime.now());
            
            if (existingKey.isPresent()) {
                if (existingKey.get().getStatus() == IdempotencyKey.TransactionStatus.SUCCESS) {
                    return objectMapper.readValue(existingKey.get().getResponse(), TransferResponse.class);
                } else if (existingKey.get().getStatus() == IdempotencyKey.TransactionStatus.PENDING) {
                    return TransferResponse.accepted(existingKey.get().getTxId());
                }
            }
            
            // Validate accounts
            if (request.getSourceAccount().equals(request.getDestinationAccount())) {
                return TransferResponse.rejected("Source and destination accounts cannot be the same");
            }
            
            // Check if source account exists and is active
            Optional<Account> sourceAccount = accountRepository
                .findByAccountIdAndStatus(request.getSourceAccount(), Account.AccountStatus.ACTIVE);
            if (sourceAccount.isEmpty()) {
                return TransferResponse.rejected("Source account not found or inactive");
            }
            
            // Check if destination account exists and is active
            Optional<Account> destAccount = accountRepository
                .findByAccountIdAndStatus(request.getDestinationAccount(), Account.AccountStatus.ACTIVE);
            if (destAccount.isEmpty()) {
                return TransferResponse.rejected("Destination account not found or inactive");
            }
            
            // Generate transaction ID
            String txId = UUID.randomUUID().toString();
            
            // Create idempotency key
            IdempotencyKey idempotencyKey = new IdempotencyKey();
            idempotencyKey.setTxId(request.getIdempotencyKey());
            idempotencyKey.setStatus(IdempotencyKey.TransactionStatus.PENDING);
            idempotencyKey.setExpiresAt(LocalDateTime.now().plusHours(24));
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Create transaction status
            TransactionStatusEntity txStatus = new TransactionStatusEntity();
            txStatus.setTxId(txId);
            txStatus.setSourceAccount(request.getSourceAccount());
            txStatus.setDestinationAccount(request.getDestinationAccount());
            txStatus.setAmount(request.getAmount());
            txStatus.setStatus(TransactionStatusEntity.Status.REQUESTED);
            transactionStatusRepository.save(txStatus);
            
            // Create transfer requested event
            TransferRequestedEvent event = new TransferRequestedEvent();
            event.setTxId(txId);
            event.setSourceAccount(request.getSourceAccount());
            event.setDestinationAccount(request.getDestinationAccount());
            event.setAmount(request.getAmount());
            event.setDescription(request.getDescription());
            event.setTimestamp(System.currentTimeMillis());
            
            // Save to outbox
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setTxId(txId);
            outboxEvent.setEventType("transfer.requested");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setPartitionKey(request.getSourceAccount());
            outboxEventRepository.save(outboxEvent);
            
            TransferResponse response = TransferResponse.accepted(txId);
            
            // Update idempotency key with response
            idempotencyKey.setResponse(objectMapper.writeValueAsString(response));
            idempotencyKey.setStatus(IdempotencyKey.TransactionStatus.SUCCESS);
            idempotencyKeyRepository.save(idempotencyKey);
            
            log.info("Transfer initiated: txId={}, source={}, dest={}, amount={}", 
                    txId, request.getSourceAccount(), request.getDestinationAccount(), request.getAmount());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error initiating transfer", e);
            return TransferResponse.rejected("Internal error: " + e.getMessage());
        }
    }
    
    public Optional<AccountBalanceResponse> getAccountBalance(String accountId) {
        return balanceRepository.findById(accountId)
            .map(balance -> {
                AccountBalanceResponse response = new AccountBalanceResponse();
                response.setAccountId(accountId);
                response.setBook(balance.getBook());
                response.setAvailable(balance.getAvailable());
                response.setOpenHold(balance.getOpenHold());
                response.setTimestamp(System.currentTimeMillis());
                return response;
            });
    }
    
    public Optional<TransactionStatusResponse> getTransactionStatus(String txId) {
        return transactionStatusRepository.findById(txId)
            .map(tx -> {
                TransactionStatusResponse response = new TransactionStatusResponse();
                response.setTxId(tx.getTxId());
                response.setSourceAccount(tx.getSourceAccount());
                response.setDestinationAccount(tx.getDestinationAccount());
                response.setAmount(tx.getAmount());
                response.setStatus(tx.getStatus().name());
                response.setErrorMessage(tx.getErrorMessage());
                response.setCreatedAt(tx.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
                response.setUpdatedAt(tx.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
                return response;
            });
    }
}