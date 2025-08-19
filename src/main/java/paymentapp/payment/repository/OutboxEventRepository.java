package paymentapp.payment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import paymentapp.payment.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAt();
    
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.processed = true, o.processedAt = :processedAt " +
           "WHERE o.id = :id")
    int markAsProcessed(@Param("id") Long id, @Param("processedAt") LocalDateTime processedAt);
}