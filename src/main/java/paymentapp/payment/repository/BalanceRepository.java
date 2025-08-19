package paymentapp.payment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import paymentapp.payment.entity.Balance;

import java.math.BigDecimal;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, String> {
    @Modifying
    @Query("UPDATE Balance b SET b.openHold = b.openHold + :amount, " +
           "b.available = b.available - :amount " +
           "WHERE b.accountId = :accountId AND b.available >= :amount")
    int createHold(@Param("accountId") String accountId, @Param("amount") BigDecimal amount);
    
    @Modifying
    @Query("UPDATE Balance b SET b.openHold = b.openHold - :amount " +
           "WHERE b.accountId = :accountId AND b.openHold >= :amount")
    int releaseHold(@Param("accountId") String accountId, @Param("amount") BigDecimal amount);
    
    @Modifying
    @Query("UPDATE Balance b SET b.book = b.book + :amount " +
           "WHERE b.accountId = :accountId")
    int creditAmount(@Param("accountId") String accountId, @Param("amount") BigDecimal amount);
    
    @Modifying
    @Query("UPDATE Balance b SET b.book = b.book - :amount " +
           "WHERE b.accountId = :accountId AND b.book >= :amount")
    int debitAmount(@Param("accountId") String accountId, @Param("amount") BigDecimal amount);
}