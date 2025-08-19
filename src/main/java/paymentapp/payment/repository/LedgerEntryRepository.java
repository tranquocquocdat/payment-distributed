package paymentapp.payment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import paymentapp.payment.entity.LedgerEntry;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTxIdOrderByCreatedAt(String txId);
    
    @Query("SELECT l FROM LedgerEntry l WHERE l.legType = 'HOLD' " +
           "AND l.status = 'SUCCESS' AND l.createdAt < :cutoffTime " +
           "AND NOT EXISTS (SELECT 1 FROM LedgerEntry l2 WHERE l2.txId = l.txId " +
           "AND l2.accountId = l.accountId AND l2.legType IN ('DEBIT', 'RELEASE'))")
    List<LedgerEntry> findOrphanedHolds(@Param("cutoffTime") LocalDateTime cutoffTime);
}