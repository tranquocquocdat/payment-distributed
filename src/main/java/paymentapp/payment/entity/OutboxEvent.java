package paymentapp.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Data
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tx_id", nullable = false, length = 50)
    private String txId;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;
    
    @Column(name = "partition_key", nullable = false, length = 50)
    private String partitionKey;
    
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}