package paymentapp.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class PaymentSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentSystemApplication.class, args);
    }
}