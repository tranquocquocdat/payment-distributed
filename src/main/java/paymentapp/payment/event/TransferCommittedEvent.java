package paymentapp.payment.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferCommittedEvent {
    private String txId;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private Long timestamp;
}