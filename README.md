# Distributed Payment System

A production-ready distributed payment system built with Spring Boot, implementing the 7-step transaction flow with event-driven architecture.

## Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2, Java 17
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Message Queue**: Apache Kafka
- **Cache**: Redis
- **CDC**: Debezium (planned)
- **Monitoring**: Spring Actuator + Prometheus
- **Testing**: TestContainers, JUnit 5

### Key Features
- ✅ **ACID Compliance** per shard
- ✅ **Event-Driven Architecture** with Kafka
- ✅ **Idempotency** protection
- ✅ **Async Balance Projection** for real-time UX
- ✅ **Automatic Reconciliation** for orphaned transactions
- ✅ **Horizontal Sharding** support
- ✅ **Circuit Breaker** patterns
- ✅ **Comprehensive Monitoring**

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- Maven 3.8+

### Running with Docker
```bash
# Clone the repository
git clone <repository-url>
cd distributed-payment-system

# Start all services
docker-compose up -d

# Check application health
curl http://localhost:8081/api/v1/actuator/health

# Access Kafka UI
open http://localhost:8080
```

### API Examples

#### Initiate Transfer
```bash
curl -X POST http://localhost:8081/api/v1/payments/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccount": "ACC001",
    "destinationAccount": "ACC002", 
    "amount": 1000.00,
    "description": "Test transfer",
    "idempotencyKey": "unique-key-123"
  }'
```

#### Check Account Balance
```bash
curl http://localhost:8081/api/v1/payments/accounts/ACC001/balance
```

#### Check Transaction Status
```bash
curl http://localhost:8081/api/v1/payments/transactions/{txId}/status
```

## Transaction Flow

### 7-Step Process
1. **INGRESS** - Accept transfer request, validate, store in outbox
2. **OUTBOX RELAY** - Publish events to Kafka with ordering guarantees
3. **HOLD** - Reserve funds using CAS operations
4. **CREDIT** - Add funds to destination account
5. **COMMIT** - Finalize transfer with debit and release
6. **BALANCE PROJECTOR** - Update balance tables asynchronously
7. **RECONCILER** - Handle timeouts and error recovery

### Event Flow
```
transfer.requested → transfer.held → transfer.credited → transfer.committed
                  ↘ transfer.rejected ↙
                  ↘ transfer.cancelled ↙
```

## Database Schema

### Core Tables
- `account_mst` - Account information
- `balances` - Real-time account balances
- `ledger_entries` - Immutable transaction log
- `outbox` - Event sourcing table
- `idempotency_keys` - Duplicate prevention
- `transaction_status` - Transaction state tracking

### Sharding Strategy
Data is partitioned by `account_id` using consistent hashing:
- Kafka partitioning by `source_account_id`
- Database sharding by `account_id % shard_count`
- Maintains ordering per account

## Monitoring & Operations

### Health Checks
- Database connectivity
- Kafka connectivity
- Redis connectivity
- Application metrics

### Metrics Available
- Transaction throughput
- Error rates by type
- Balance reconciliation status
- Kafka lag monitoring

### Reconciliation
- Automatic cleanup of orphaned holds (default: 5 minutes)
- Failed transaction retry with exponential backoff
- Manual intervention tools for edge cases

## Configuration

### Environment Variables
```bash
# Database
DB_HOST=mysql
DB_PORT=3306
DB_NAME=payment_db
DB_USERNAME=payment_user
DB_PASSWORD=payment_pass

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Kafka
KAFKA_SERVERS=kafka:29092

# Application
CURRENT_SHARD=1
SERVER_PORT=8080
```

### Scaling Considerations
- **Horizontal**: Add more application instances with different shard assignments
- **Database**: Use read replicas for balance queries
- **Kafka**: Increase partition count for higher throughput
- **Cache**: Redis Cluster for high availability

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests with TestContainers
```bash
mvn verify
```

### Load Testing
```bash
# Use included JMeter scripts or k6 tests
k6 run tests/load/transfer-test.js
```

## Security Considerations

### Current Implementation
- Input validation with Bean Validation
- SQL injection prevention with JPA
- Idempotency key validation

### Production Recommendations
- JWT/OAuth2 authentication
- API rate limiting
- Database encryption at rest
- Network security (VPC, firewalls)
- Audit logging
- Secret management (HashiCorp Vault)

## Deployment

### Production Checklist
- [ ] Enable SSL/TLS termination
- [ ] Configure proper logging levels
- [ ] Set up monitoring and alerting
- [ ] Database backup strategy
- [ ] Disaster recovery plan
- [ ] Load balancer configuration
- [ ] Auto-scaling policies

### Kubernetes Deployment
```yaml
# Helm chart available in ./k8s/
helm install payment-system ./k8s/payment-system-chart
```

## Troubleshooting

### Common Issues
1. **Orphaned Holds**: Check reconciler logs and configuration
2. **Kafka Connectivity**: Verify bootstrap servers and topic creation
3. **Database Locks**: Monitor long-running transactions
4. **Balance Inconsistencies**: Run balance reconciliation job

### Debug Commands
```bash
# Check Kafka topics
docker exec payment-kafka kafka-topics --list --bootstrap-server localhost:9092

# Check database connections
docker exec payment-mysql mysql -u payment_user -p payment_db -e "SHOW PROCESSLIST;"

# Application logs
docker logs payment-application -f
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Standards
- Java Google Style Guide
- Test coverage > 80%
- All tests must pass
- Documentation updates required

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, email dat97it@gmail.com or create an issue in the repository.

---

**⚡ Built with performance, reliability, and scalability in mind ⚡**