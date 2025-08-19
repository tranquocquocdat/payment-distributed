package paymentapp.payment.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRejectedEvent {
    private String txId;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private String reason;
    private Long timestamp;
}