package paymentapp.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    
    @Bean
    public NewTopic transferRequestedTopic() {
        return TopicBuilder.name("transfer.requested")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transferHeldTopic() {
        return TopicBuilder.name("transfer.held")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transferCreditedTopic() {
        return TopicBuilder.name("transfer.credited")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transferCommittedTopic() {
        return TopicBuilder.name("transfer.committed")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transferRejectedTopic() {
        return TopicBuilder.name("transfer.rejected")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transferCancelledTopic() {
        return TopicBuilder.name("transfer.cancelled")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic ledgerHoldCreatedTopic() {
        return TopicBuilder.name("ledger.hold_created")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic ledgerHoldReleasedTopic() {
        return TopicBuilder.name("ledger.hold_released")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic ledgerCreditPostedTopic() {
        return TopicBuilder.name("ledger.credit_posted")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic ledgerDebitPostedTopic() {
        return TopicBuilder.name("ledger.debit_posted")
            .partitions(3)
            .replicas(1)
            .build();
    }
}