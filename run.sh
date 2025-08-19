#!/bin/bash

# Distributed Payment System - Complete Startup Script

echo "🚀 Starting Distributed Payment System..."
echo "=========================================="

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to wait for service
wait_for_service() {
    local service=$1
    local host=$2
    local port=$3
    local max_attempts=30
    local attempt=1

    echo "⏳ Waiting for $service to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if timeout 1 bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null; then
            echo "✅ $service is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: $service not ready yet..."
        sleep 5
        ((attempt++))
    done

    echo "❌ $service failed to start after $max_attempts attempts"
    return 1
}

# Check prerequisites
echo "🔍 Checking prerequisites..."

if ! command_exists docker; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✅ All prerequisites met"

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v

# Start infrastructure services in order
echo "🏗️ Starting infrastructure services..."

# Start MySQL first
echo "📊 Starting MySQL..."
docker-compose up -d mysql
sleep 10

# Wait for MySQL
if ! wait_for_service "MySQL" "localhost" "3307"; then
    echo "📋 MySQL logs:"
    docker logs payment-mysql --tail 20
    exit 1
fi

# Start Redis
echo "🔴 Starting Redis..."
docker-compose up -d redis
sleep 5

# Wait for Redis
if ! wait_for_service "Redis" "localhost" "6380"; then
    echo "📋 Redis logs:"
    docker logs payment-redis --tail 20
    exit 1
fi

# Start Zookeeper
echo "🦓 Starting Zookeeper..."
docker-compose up -d zookeeper
sleep 10

# Wait for Zookeeper
if ! wait_for_service "Zookeeper" "localhost" "2182"; then
    echo "📋 Zookeeper logs:"
    docker logs payment-zookeeper --tail 20
    exit 1
fi

# Start Kafka
echo "📨 Starting Kafka..."
docker-compose up -d kafka
sleep 20

# Wait for Kafka using internal health check
echo "⏳ Waiting for Kafka to be ready..."
max_attempts=20
attempt=1

while [ $attempt -le $max_attempts ]; do
    if docker exec payment-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        echo "✅ Kafka is ready!"
        break
    fi
    echo "Attempt $attempt/$max_attempts: Kafka not ready yet..."
    sleep 10
    ((attempt++))
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ Kafka failed to start properly"
    echo "📋 Kafka logs:"
    docker logs payment-kafka --tail 30
    exit 1
fi

# Start Kafka UI
echo "🖥️ Starting Kafka UI..."
docker-compose up -d kafka-ui
sleep 5

# Build and start the application
echo "🔨 Building and starting the application..."
echo "📦 Building Docker image..."
docker-compose build payment-app

if [ $? -eq 0 ]; then
    echo "✅ Docker image built successfully"
else
    echo "❌ Docker image build failed"
    exit 1
fi

echo "🚀 Starting application..."
docker-compose up -d payment-app

# Wait for application to be ready
echo "⏳ Waiting for application to start..."
sleep 30

# Check application health
echo "🏥 Checking application health..."
max_attempts=15
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s http://localhost:8081/api/v1/actuator/health > /dev/null 2>&1; then
        echo "✅ Application is healthy!"
        break
    else
        echo "Attempt $attempt/$max_attempts: Application not ready yet..."
        sleep 15
        ((attempt++))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ Application failed to start properly"
    echo "📋 Application logs:"
    docker logs payment-application --tail 50
    echo ""
    echo "🔍 Debug info:"
    docker-compose ps
    exit 1
fi

# Final status check
echo ""
echo "📊 System Status:"
echo "=================="
docker-compose ps

echo ""
echo "🎉 Distributed Payment System is now running!"
echo "=============================================="
echo ""
echo "📍 Application URLs:"
echo "  • API: http://localhost:8081/api/v1"
echo "  • Health: http://localhost:8081/api/v1/actuator/health"
echo "  • Kafka UI: http://localhost:8080"
echo ""
echo "🧪 Quick Test Commands:"
echo "curl http://localhost:8081/api/v1/actuator/health"
echo ""
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
echo "curl http://localhost:8081/api/v1/payments/accounts/ACC001/balance"
echo ""
echo "📱 Monitor Commands:"
echo "docker logs payment-application -f    # Application logs"
echo "docker-compose ps                    # Service status"
echo "docker stats                         # Resource usage"
echo ""
echo "🛑 Stop System:"
echo "docker-compose down                  # Stop all services"
echo "docker-compose down -v              # Stop and remove volumes"