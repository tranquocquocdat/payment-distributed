package paymentapp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_status")
@Data
public class TransactionStatusEntity {
    @Id
    @Column(name = "tx_id", length = 50)
    private String txId;
    
    @Column(name = "source_account", nullable = false, length = 20)
    private String sourceAccount;
    
    @Column(name = "destination_account", nullable = false, length = 20)
    private String destinationAccount;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        REQUESTED, HELD, CREDITED, COMMITTED, REJECTED, CANCELLED
    }
}