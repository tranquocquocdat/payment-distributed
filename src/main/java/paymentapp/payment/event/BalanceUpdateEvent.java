package paymentapp.payment.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BalanceUpdateEvent {
    private String accountId;
    private String txId;
    private String operation; // HOLD_CREATED, HOLD_RELEASED, CREDIT_POSTED, DEBIT_POSTED
    private BigDecimal amount;
    private Long timestamp;
}