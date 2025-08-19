package paymentapp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balances")
@Data
public class Balance {
    @Id
    @Column(name = "account_id", length = 20)
    private String accountId;
    
    @Column(name = "book", precision = 15, scale = 2, nullable = false)
    private BigDecimal book = BigDecimal.ZERO;
    
    @Column(name = "available", precision = 15, scale = 2, nullable = false)
    private BigDecimal available = BigDecimal.ZERO;
    
    @Column(name = "open_hold", precision = 15, scale = 2, nullable = false)
    private BigDecimal openHold = BigDecimal.ZERO;
    
    @Version
    @Column(name = "version")
    private Long version = 0L;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToOne
    @JoinColumn(name = "account_id")
    private Account account;
}