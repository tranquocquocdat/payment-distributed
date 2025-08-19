package paymentapp.payment.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferHeldEvent {
    private String txId;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private Long timestamp;
}