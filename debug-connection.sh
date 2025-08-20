#!/bin/bash

echo "🔍 Debugging Payment System Connections"
echo "========================================"

# Function to test connection
test_connection() {
    local service=$1
    local host=$2
    local port=$3

    echo "Testing $service connection..."
    if timeout 3 bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null; then
        echo "✅ $service is reachable at $host:$port"
    else
        echo "❌ $service is NOT reachable at $host:$port"
    fi
    echo ""
}

# Check if containers are running
echo "📊 Container Status:"
docker-compose ps
echo ""

# Test external connections (from host)
echo "🌐 Testing External Connections (from host):"
test_connection "MySQL" "localhost" "3307"
test_connection "Redis" "localhost" "6380"
test_connection "Kafka" "localhost" "9093"
test_connection "Application" "localhost" "8081"

# Test internal connections (from application container)
echo "🔗 Testing Internal Connections (from app container):"
if docker ps | grep -q payment-application; then
    echo "Testing Redis connection from app container..."
    docker exec payment-application sh -c "timeout 3 bash -c 'echo >/dev/tcp/redis/6379'" 2>/dev/null && \
        echo "✅ Redis is reachable from app container" || \
        echo "❌ Redis is NOT reachable from app container"

    echo "Testing MySQL connection from app container..."
    docker exec payment-application sh -c "timeout 3 bash -c 'echo >/dev/tcp/mysql/3306'" 2>/dev/null && \
        echo "✅ MySQL is reachable from app container" || \
        echo "❌ MySQL is NOT reachable from app container"

    echo "Testing Kafka connection from app container..."
    docker exec payment-application sh -c "timeout 3 bash -c 'echo >/dev/tcp/kafka/29092'" 2>/dev/null && \
        echo "✅ Kafka is reachable from app container" || \
        echo "❌ Kafka is NOT reachable from app container"
else
    echo "❌ Application container is not running"
fi
echo ""

# Check networks
echo "🌐 Docker Networks:"
docker network ls | grep payment
echo ""

# Check container environment variables
echo "🔧 Application Environment Variables:"
if docker ps | grep -q payment-application; then
    docker exec payment-application env | grep -E "(REDIS|DB|KAFKA)" | sort
else
    echo "❌ Application container is not running"
fi
echo ""

# Show recent logs
echo "📋 Recent Application Logs:"
if docker ps | grep -q payment-application; then
    docker logs payment-application --tail 20
else
    echo "❌ Application container is not running"
fi
echo ""

# Redis-specific checks
echo "🔴 Redis Specific Checks:"
if docker ps | grep -q payment-redis; then
    echo "✅ Redis container is running"
    echo "Redis container details:"
    docker inspect payment-redis | grep -E "(IPAddress|Networks)" -A 5

    echo ""
    echo "Testing Redis ping from host:"
    docker exec payment-redis redis-cli ping 2>/dev/null && \
        echo "✅ Redis is responding to ping" || \
        echo "❌ Redis is not responding to ping"

    if docker ps | grep -q payment-application; then
        echo ""
        echo "Testing Redis ping from app container:"
        docker exec payment-application sh -c "echo 'PING' | nc redis 6379" 2>/dev/null && \
            echo "✅ Redis is responding from app container" || \
            echo "❌ Redis is not responding from app container"
    fi
else
    echo "❌ Redis container is not running"
fi

echo ""
echo "🏁 Debug Complete!"
echo ""
echo "💡 Common Solutions:"
echo "1. If Redis is not reachable: Check REDIS_HOST environment variable"
echo "2. If containers can't communicate: Check they're on the same network"
echo "3. If application won't start: Check application.yml Redis configuration"
echo "4. Run: docker-compose down -v && docker-compose up -d to reset"