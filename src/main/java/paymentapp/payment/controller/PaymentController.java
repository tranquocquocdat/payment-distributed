package paymentapp.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paymentapp.payment.dto.AccountBalanceResponse;
import paymentapp.payment.dto.TransactionStatusResponse;
import paymentapp.payment.dto.TransferRequest;
import paymentapp.payment.dto.TransferResponse;
import paymentapp.payment.service.PaymentService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> initiateTransfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = paymentService.initiateTransfer(request);
        
        if ("ACCEPTED".equals(response.getStatus())) {
            return ResponseEntity.accepted().body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable String accountId) {
        return paymentService.getAccountBalance(accountId)
            .map(balance -> ResponseEntity.ok(balance))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/transactions/{txId}/status")
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(@PathVariable String txId) {
        return paymentService.getTransactionStatus(txId)
            .map(status -> ResponseEntity.ok(status))
            .orElse(ResponseEntity.notFound().build());
    }
}