#!/bin/bash
set -e

echo "Waiting for Kafka to be ready..."
cub kafka-ready -b kafka:29092 1 60

echo "Creating Kafka topics..."

# Order events - partitioned by customerId for ordering
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists \
  --topic order-events \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Asset events - partitioned by customerId
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists \
  --topic asset-events \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Notification events
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists \
  --topic notification-events \
  --partitions 2 \
  --replication-factor 1 \
  --config retention.ms=86400000

# Audit events - high retention
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists \
  --topic audit-events \
  --partitions 2 \
  --replication-factor 1 \
  --config retention.ms=2592000000

# Dead letter queue
kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists \
  --topic dlq-events \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=2592000000

echo "Kafka topics created successfully!"
kafka-topics --bootstrap-server kafka:29092 --list
