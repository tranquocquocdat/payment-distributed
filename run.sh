#!/bin/bash

# Distributed Payment System - Startup Script

echo "🚀 Starting Distributed Payment System..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Stop any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down

# Start infrastructure services first
echo "🏗️ Starting infrastructure services..."
docker-compose up -d mysql redis zookeeper kafka

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check MySQL health
echo "📊 Checking MySQL health..."
until docker exec payment-mysql mysqladmin ping -h localhost --silent; do
    echo "Waiting for MySQL..."
    sleep 5
done

# Check Kafka health
echo "📨 Checking Kafka health..."
until docker exec payment-kafka kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "Waiting for Kafka..."
    sleep 5
done

# Start Kafka UI
echo "🖥️ Starting Kafka UI..."
docker-compose up -d kafka-ui

# Build and start the application
echo "🔨 Building and starting the application..."
docker-compose up -d payment-app

# Wait for application to be ready
echo "⏳ Waiting for application to start..."
sleep 45

# Check application health
echo "🏥 Checking application health..."
max_attempts=12
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s http://localhost:8081/api/v1/actuator/health > /dev/null; then
        echo "✅ Application is healthy!"
        break
    else
        echo "Attempt $attempt/$max_attempts: Application not ready yet..."
        sleep 10
        ((attempt++))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ Application failed to start properly. Check logs:"
    echo "docker logs payment-application"
    exit 1
fi

echo ""
echo "🎉 Distributed Payment System is now running!"
echo ""
echo "📍 Application URLs:"
echo "  • API: http://localhost:8081/api/v1"
echo "  • Health Check: http://localhost:8081/api/v1/actuator/health"
echo "  • Kafka UI: http://localhost:8080"
echo ""
echo "🧪 Test the API:"
echo "curl -X POST http://localhost:8081/api/v1/payments/transfer \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{"
echo "    \"sourceAccount\": \"ACC001\","
echo "    \"destinationAccount\": \"ACC002\","
echo "    \"amount\": 1000.00,"
echo "    \"description\": \"Test transfer\","
echo "    \"idempotencyKey\": \"test-key-$(date +%s)\""
echo "  }'"
echo ""
echo "📊 Check account balance:"
echo "curl http://localhost:8081/api/v1/payments/accounts/ACC001/balance"
echo ""
echo "📱 Monitor logs:"
echo "docker logs payment-application -f"
echo ""
echo "🛑 To stop the system:"
echo "docker-compose down"