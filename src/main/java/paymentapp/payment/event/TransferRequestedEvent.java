package paymentapp.payment.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequestedEvent {
    private String txId;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private String description;
    private Long timestamp;
}