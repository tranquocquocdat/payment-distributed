package paymentapp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tx_id", "account_id", "leg_type"}))
@Data
@EqualsAndHashCode(callSuper = false)
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tx_id", nullable = false, length = 50)
    private String txId;
    
    @Column(name = "account_id", nullable = false, length = 20)
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", nullable = false)
    private LegType legType;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum LegType {
        HOLD, RELEASE, DEBIT, CREDIT
    }
    
    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}