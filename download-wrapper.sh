#!/bin/bash

echo "📥 Downloading Maven Wrapper files..."

# Create wrapper directory
mkdir -p .mvn/wrapper

# Download maven-wrapper.jar
echo "Downloading maven-wrapper.jar..."
curl -L -o .mvn/wrapper/maven-wrapper.jar \
    https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar

if [ $? -eq 0 ]; then
    echo "✅ Successfully downloaded maven-wrapper.jar"
else
    echo "❌ Failed to download maven-wrapper.jar"
    exit 1
fi

# Make mvnw executable
chmod +x mvnw
chmod +x mvnw.cmd

echo "✅ Maven wrapper setup complete!"

# Verify
echo "🔍 Checking files..."
ls -la .mvn/wrapper/
ls -la mvnw*

echo "🧪 Testing Maven wrapper..."
./mvnw --version