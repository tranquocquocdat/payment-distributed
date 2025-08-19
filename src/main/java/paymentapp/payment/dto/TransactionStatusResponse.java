package paymentapp.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionStatusResponse {
    private String txId;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private String status;
    private String errorMessage;
    private Long createdAt;
    private Long updatedAt;
}