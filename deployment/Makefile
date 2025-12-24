# =============================================================================
# Brokage System - Deployment Makefile
# =============================================================================
# One-click demo: make up
# =============================================================================

.PHONY: help up down restart logs ps clean build-backend

JAVA_HOME_PATH := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Default target
help:
	@echo "=============================================="
	@echo "Brokage System - One-Click Demo"
	@echo "=============================================="
	@echo ""
	@echo "Quick Start:"
	@echo "  make up              - Start everything (one-click demo)"
	@echo "  make down            - Stop everything"
	@echo "  make clean           - Reset everything (removes data)"
	@echo ""
	@echo "Development:"
	@echo "  make up-infra        - Start infrastructure only"
	@echo "  make up-apps         - Start apps only (infra must be running)"
	@echo "  make logs            - View all logs"
	@echo "  make logs-apps       - View application logs"
	@echo "  make ps              - List running services"
	@echo "  make status          - Check all service health"
	@echo ""
	@echo "URLs:"
	@echo "  Grafana:     http://localhost:3001 (admin/admin123)"
	@echo "  Keycloak:    http://localhost:8180 (admin/admin123)"
	@echo "  Traefik:     http://localhost:8090"
	@echo "  Kafka UI:    http://localhost:8089"
	@echo ""
	@echo "API Services:"
	@echo "  Order Service:        http://localhost:7081/actuator/health"
	@echo "  Asset Service:        http://localhost:7082/actuator/health"
	@echo "  Customer Service:     http://localhost:7083/actuator/health"
	@echo "  Order Processor:      http://localhost:7084/actuator/health"
	@echo "  Notification Service: http://localhost:7085/actuator/health"
	@echo "  Audit Service:        http://localhost:7086/actuator/health"
	@echo ""
	@echo "Demo Users (password: password123):"
	@echo "  admin@brokage.com    - ADMIN role"
	@echo "  broker1@brokage.com  - BROKER role"
	@echo "  customer1@brokage.com - CUSTOMER role"

# =============================================================================
# ONE-CLICK DEMO
# =============================================================================

# Build backend JARs
build-backend:
	@echo "=========================================="
	@echo "Building backend services..."
	@echo "=========================================="
	cd ../backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew bootJar --parallel -q
	@echo "Backend build complete!"

# Start everything (ONE-CLICK DEMO)
up: build-backend
	@echo "=========================================="
	@echo "Starting Brokage System..."
	@echo "=========================================="
	@echo ""
	@echo "Step 1: Starting infrastructure..."
	docker compose up -d postgres mongodb redis zookeeper
	@echo "Waiting for databases..."
	@sleep 5
	@echo ""
	@echo "Step 2: Starting Kafka..."
	docker compose up -d kafka
	@sleep 10
	docker compose up -d schema-registry kafka-init
	@echo ""
	@echo "Step 3: Starting Keycloak & Monitoring..."
	docker compose up -d keycloak lgtm traefik kafka-ui
	@echo "Waiting for Keycloak..."
	@sleep 15
	docker compose up -d keycloak-init
	@echo ""
	@echo "Step 4: Building & starting applications..."
	docker compose up -d --build order-service asset-service customer-service order-processor notification-service audit-service
	@echo ""
	@echo "=========================================="
	@echo "Brokage System Starting!"
	@echo "=========================================="
	@echo ""
	@echo "Services are starting up. This may take 1-2 minutes."
	@echo "Run 'make status' to check service health."
	@echo "Run 'make logs-apps' to view application logs."
	@echo ""
	@echo "URLs:"
	@echo "  API Gateway: http://localhost (via Traefik)"
	@echo "  Grafana:     http://localhost:3001"
	@echo "  Keycloak:    http://localhost:8180"
	@echo ""

# =============================================================================
# INFRASTRUCTURE COMMANDS
# =============================================================================

# Start infrastructure only
up-infra:
	@echo "Starting infrastructure..."
	docker compose up -d postgres mongodb redis zookeeper kafka schema-registry keycloak lgtm traefik kafka-ui kafka-init keycloak-init
	@echo "Infrastructure services starting..."

# Start application services only
up-apps: build-backend
	@echo "Starting application services..."
	docker compose up -d --build order-service asset-service customer-service order-processor notification-service audit-service
	@echo "Application services starting..."

# =============================================================================
# LIFECYCLE COMMANDS
# =============================================================================

# Stop all services
down:
	docker compose down

# Restart all services
restart:
	docker compose restart

# Restart apps only
restart-apps:
	docker compose restart order-service asset-service customer-service order-processor notification-service audit-service

# =============================================================================
# LOGGING COMMANDS
# =============================================================================

logs:
	docker compose logs -f

logs-apps:
	docker compose logs -f order-service asset-service customer-service order-processor notification-service audit-service

logs-kafka:
	docker compose logs -f kafka zookeeper schema-registry kafka-init

logs-keycloak:
	docker compose logs -f keycloak keycloak-init

logs-lgtm:
	docker compose logs -f lgtm

# =============================================================================
# STATUS & MONITORING
# =============================================================================

ps:
	docker compose ps

# Check all service health
status:
	@echo "=========================================="
	@echo "Service Health Check"
	@echo "=========================================="
	@echo ""
	@echo "Infrastructure:"
	@docker compose exec -T postgres pg_isready -U brokage > /dev/null 2>&1 && echo "  ✓ PostgreSQL" || echo "  ✗ PostgreSQL"
	@docker compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" -u brokage -p brokage123 --authenticationDatabase admin --quiet > /dev/null 2>&1 && echo "  ✓ MongoDB" || echo "  ✗ MongoDB"
	@docker compose exec -T redis redis-cli -a brokage123 ping > /dev/null 2>&1 && echo "  ✓ Redis" || echo "  ✗ Redis"
	@docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "  ✓ Kafka" || echo "  ✗ Kafka"
	@curl -sf http://localhost:8091/subjects > /dev/null && echo "  ✓ Schema Registry" || echo "  ✗ Schema Registry"
	@echo ""
	@echo "Platform Services:"
	@curl -sf http://localhost:8180/health/ready > /dev/null && echo "  ✓ Keycloak" || echo "  ✗ Keycloak"
	@curl -sf http://localhost:3001/api/health > /dev/null && echo "  ✓ Grafana (LGTM)" || echo "  ✗ Grafana (LGTM)"
	@curl -sf http://localhost:8090/api/rawdata > /dev/null && echo "  ✓ Traefik" || echo "  ✗ Traefik"
	@echo ""
	@echo "Application Services:"
	@curl -sf http://localhost:7081/actuator/health > /dev/null && echo "  ✓ Order Service" || echo "  ✗ Order Service"
	@curl -sf http://localhost:7082/actuator/health > /dev/null && echo "  ✓ Asset Service" || echo "  ✗ Asset Service"
	@curl -sf http://localhost:7083/actuator/health > /dev/null && echo "  ✓ Customer Service" || echo "  ✗ Customer Service"
	@curl -sf http://localhost:7084/actuator/health > /dev/null && echo "  ✓ Order Processor" || echo "  ✗ Order Processor"
	@curl -sf http://localhost:7085/actuator/health > /dev/null && echo "  ✓ Notification Service" || echo "  ✗ Notification Service"
	@curl -sf http://localhost:7086/actuator/health > /dev/null && echo "  ✓ Audit Service" || echo "  ✗ Audit Service"
	@echo ""

# =============================================================================
# CLEANUP
# =============================================================================

# Clean up everything including volumes
clean:
	@echo "Stopping and removing all containers, volumes, and images..."
	docker compose down -v --rmi local
	@echo "Clean complete!"

# =============================================================================
# KAFKA UTILITIES
# =============================================================================

list-topics:
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --list

describe-topics:
	docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --describe
