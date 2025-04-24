#!/bin/bash

# Building the JAR file using Maven
echo "Building project with Maven..."
mvn clean package

# Check if Maven build was successful
if [ $? -ne 0 ]; then
    echo "Maven build failed. Exiting."
    exit 1
fi
echo "Maven build completed successfully."

# Stopping and removing existing containers
echo "Stopping and removing existing containers if they exist..."
docker rm -f grafana-demo-1 grafana-demo-2 || true

# Starting services using Docker Compose with build
echo "Starting services with Docker Compose..."
docker compose up --build

# Capturing exit code
exit_code=$?

# Check if the exit code is 130 (Ctrl+C)
if [ $exit_code -eq 130 ]; then
    echo "Docker Compose process was interrupted (Ctrl+C). Exiting gracefully."
    exit 0
fi

# Checking if Docker Compose started successfully
if [ $exit_code -ne 0 ]; then
    echo "Failed to start Docker Compose services. Exiting."
    exit 1
fi

echo "Docker Compose services started successfully."