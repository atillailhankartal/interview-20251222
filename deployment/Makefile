# =============================================================================
# Brokage System - Makefile
# =============================================================================

.PHONY: help up down restart logs ps clean test-connections

# Default target
help:
	@echo "Brokage System - Development Environment"
	@echo ""
	@echo "Usage:"
	@echo "  make up              - Start all services"
	@echo "  make up-infra        - Start infrastructure only (DB, Kafka, etc.)"
	@echo "  make down            - Stop all services"
	@echo "  make restart         - Restart all services"
	@echo "  make logs            - View all logs"
	@echo "  make logs-kafka      - View Kafka logs"
	@echo "  make ps              - List running services"
	@echo "  make clean           - Stop and remove volumes"
	@echo "  make test-connections - Test all service connections"
	@echo ""
	@echo "URLs:"
	@echo "  Grafana:     http://localhost:3001 (admin/admin123)"
	@echo "  Keycloak:    http://localhost:8180 (admin/admin123)"
	@echo "  Traefik:     http://localhost:8090"
	@echo "  Kafka UI:    http://localhost:8083"
	@echo ""
	@echo "Custom Ports:"
	@echo "  PostgreSQL:  5433"
	@echo "  MongoDB:     27018"
	@echo "  Redis:       6380"
	@echo "  Kafka:       9093"

# Start all services
up:
	docker compose up -d
	@echo ""
	@echo "Services starting... Use 'make ps' to check status"
	@echo "Use 'make logs' to view logs"

# Start infrastructure only
up-infra:
	docker compose up -d postgres mongodb redis zookeeper kafka schema-registry lgtm
	@echo ""
	@echo "Infrastructure services starting..."

# Stop all services
down:
	docker compose down

# Restart all services
restart:
	docker compose restart

# View logs
logs:
	docker compose logs -f

logs-kafka:
	docker compose logs -f kafka zookeeper schema-registry

logs-keycloak:
	docker compose logs -f keycloak

logs-lgtm:
	docker compose logs -f lgtm

# List running services
ps:
	docker compose ps

# Clean up everything including volumes
clean:
	docker compose down -v
	@echo "All containers and volumes removed"

# Test connections
test-connections:
	@echo "Testing PostgreSQL..."
	@docker compose exec -T postgres pg_isready -U brokage || echo "PostgreSQL not ready"
	@echo ""
	@echo "Testing MongoDB..."
	@docker compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" -u brokage -p brokage123 --authenticationDatabase admin --quiet || echo "MongoDB not ready"
	@echo ""
	@echo "Testing Redis..."
	@docker compose exec -T redis redis-cli -a brokage123 ping || echo "Redis not ready"
	@echo ""
	@echo "Testing Kafka..."
	@docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "Kafka is ready" || echo "Kafka not ready"
	@echo ""
	@echo "Testing Schema Registry..."
	@curl -s http://localhost:8082/subjects > /dev/null && echo "Schema Registry is ready" || echo "Schema Registry not ready"
	@echo ""
	@echo "Testing Keycloak..."
	@curl -s http://localhost:8180/health/ready > /dev/null && echo "Keycloak is ready" || echo "Keycloak not ready"
	@echo ""
	@echo "Testing Grafana (LGTM)..."
	@curl -s http://localhost:3001/api/health > /dev/null && echo "Grafana is ready" || echo "Grafana not ready"

# Create Kafka topics
create-topics:
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic order-events --partitions 3 --replication-factor 1
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic asset-events --partitions 3 --replication-factor 1
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic notification-events --partitions 2 --replication-factor 1
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic audit-events --partitions 2 --replication-factor 1
	@echo "Kafka topics created"

# List Kafka topics
list-topics:
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --list
