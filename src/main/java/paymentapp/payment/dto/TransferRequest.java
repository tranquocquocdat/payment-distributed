package paymentapp.payment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Source account is required")
    @Size(max = 20, message = "Source account must not exceed 20 characters")
    private String sourceAccount;
    
    @NotBlank(message = "Destination account is required")
    @Size(max = 20, message = "Destination account must not exceed 20 characters")
    private String destinationAccount;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum limit")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}