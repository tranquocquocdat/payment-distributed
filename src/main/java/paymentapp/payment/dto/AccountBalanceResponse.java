package paymentapp.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountBalanceResponse {
    private String accountId;
    private BigDecimal book;
    private BigDecimal available;
    private BigDecimal openHold;
    private Long timestamp;
}