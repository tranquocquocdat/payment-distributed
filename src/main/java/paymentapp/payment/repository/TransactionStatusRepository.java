package paymentapp.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import paymentapp.payment.entity.TransactionStatusEntity;

import java.util.List;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatusEntity, String> {
    List<TransactionStatusEntity> findBySourceAccountOrderByCreatedAtDesc(String sourceAccount);
    List<TransactionStatusEntity> findByDestinationAccountOrderByCreatedAtDesc(String destinationAccount);
}