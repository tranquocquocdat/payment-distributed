package paymentapp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
@Data
public class IdempotencyKey {
    @Id
    @Column(name = "tx_id", length = 50)
    private String txId;
    
    @Column(name = "request_hash", length = 64)
    private String requestHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "response", columnDefinition = "JSON")
    private String response;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}