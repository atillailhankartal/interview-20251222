# =============================================================================
# Brokage Trading Platform - Deployment
# =============================================================================
# Zero-Config, Cross-Platform Deployment System
# =============================================================================

.PHONY: help start start-backend up down restart logs ps clean build-backend status

# Colors
RED    := \033[0;31m
GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
CYAN   := \033[0;36m
BOLD   := \033[1m
NC     := \033[0m

# Accept JAVA_HOME from parent Makefile or environment
JAVA_HOME ?= $(shell echo $$JAVA_HOME)

# =============================================================================
# HELP
# =============================================================================

help:
	@echo ""
	@echo "$(BOLD)$(CYAN)Brokage Trading Platform - Deployment$(NC)"
	@echo ""
	@echo "$(BOLD)Quick Start:$(NC)"
	@echo "  make start           - Start everything (one-click demo)"
	@echo "  make start-backend   - Start without frontend (for local UI dev)"
	@echo "  make down            - Stop everything"
	@echo "  make clean           - Reset everything (removes data)"
	@echo ""
	@echo "$(BOLD)Development:$(NC)"
	@echo "  make up-infra        - Start infrastructure only"
	@echo "  make up-apps         - Start apps only (infra must be running)"
	@echo "  make logs            - View all logs"
	@echo "  make logs-apps       - View application logs"
	@echo "  make ps              - List running services"
	@echo "  make status          - Check all service health"
	@echo ""
	@echo "$(BOLD)URLs:$(NC)"
	@echo "  Frontend:    http://localhost:4000"
	@echo "  API Gateway: http://localhost:4500"
	@echo "  Grafana:     http://localhost:3001 (admin/admin123)"
	@echo "  Keycloak:    http://localhost:8180 (admin/admin123)"
	@echo "  Traefik:     http://localhost:8090 (dashboard)"
	@echo "  Kafka UI:    http://localhost:8089"
	@echo ""
	@echo "$(BOLD)API Services:$(NC)"
	@echo "  Order Service:        http://localhost:7081/actuator/health"
	@echo "  Asset Service:        http://localhost:7082/actuator/health"
	@echo "  Customer Service:     http://localhost:7083/actuator/health"
	@echo "  Order Processor:      http://localhost:7084/actuator/health"
	@echo "  Notification Service: http://localhost:7085/actuator/health"
	@echo "  Audit Service:        http://localhost:7086/actuator/health"
	@echo "  Web API:              http://localhost:7087/actuator/health"
	@echo ""
	@echo "$(BOLD)Demo Users$(NC) (password: password123):"
	@echo "  admin@brokage.com    - ADMIN role"
	@echo "  broker1@brokage.com  - BROKER role"
	@echo "  customer1@brokage.com - CUSTOMER role"

# =============================================================================
# BUILD BACKEND
# =============================================================================

# Build with local Java (if available)
build-backend:
	@echo "$(BOLD)Building backend services...$(NC)"
	@if [ -n "$(JAVA_HOME)" ] && [ -x "$(JAVA_HOME)/bin/java" ]; then \
		echo "  Using Java: $(JAVA_HOME)"; \
		cd ../backend && JAVA_HOME=$(JAVA_HOME) ./gradlew bootJar --parallel -q; \
	else \
		echo "$(YELLOW)  Java not found locally, using Docker build...$(NC)"; \
		$(MAKE) build-backend-docker; \
	fi
	@echo "$(GREEN)  Backend build complete!$(NC)"

# Build using Docker (no local Java required)
build-backend-docker:
	@echo "$(BOLD)Building backend with Docker (no local Java required)...$(NC)"
	docker run --rm \
		-v "$(shell cd .. && pwd)/backend:/app" \
		-v "$(HOME)/.gradle:/root/.gradle" \
		-w /app \
		eclipse-temurin:17-jdk \
		./gradlew bootJar --parallel -q
	@echo "$(GREEN)  Docker build complete!$(NC)"

# =============================================================================
# ONE-CLICK START (start = build + up)
# =============================================================================

start: build-backend
	@echo ""
	@echo "$(BOLD)$(CYAN)Starting Brokage Trading Platform...$(NC)"
	@echo ""
	@echo "$(YELLOW)[1/6]$(NC) Starting databases (PostgreSQL, MongoDB, Redis)..."
	@docker compose up -d postgres mongodb redis zookeeper
	@echo "      Waiting for databases to be ready..."
	@sleep 5
	@echo ""
	@echo "$(YELLOW)[2/6]$(NC) Starting Kafka..."
	@docker compose up -d kafka
	@echo "      Waiting for Kafka to be ready..."
	@sleep 10
	@docker compose up -d schema-registry kafka-init
	@echo ""
	@echo "$(YELLOW)[3/6]$(NC) Starting Keycloak (Identity Provider)..."
	@docker compose up -d keycloak lgtm traefik kafka-ui
	@echo "      Waiting for Keycloak to be ready..."
	@sleep 15
	@docker compose up -d keycloak-init
	@echo ""
	@echo "$(YELLOW)[4/6]$(NC) Starting backend microservices..."
	@docker compose up -d --build order-service asset-service customer-service order-processor notification-service audit-service web-api
	@echo ""
	@echo "$(YELLOW)[5/6]$(NC) Starting frontend..."
	@docker compose up -d --build web-client
	@echo ""
	@echo "$(YELLOW)[6/6]$(NC) Waiting for services to be healthy..."
	@sleep 10
	@echo ""
	@echo "$(BOLD)$(GREEN)=========================================$(NC)"
	@echo "$(BOLD)$(GREEN)  Brokage Trading Platform is Ready!   $(NC)"
	@echo "$(BOLD)$(GREEN)=========================================$(NC)"
	@echo ""
	@echo "$(BOLD)Access URLs:$(NC)"
	@echo "  $(CYAN)Frontend:$(NC)     http://localhost:4000"
	@echo "  $(CYAN)API Gateway:$(NC)  http://localhost:4500"
	@echo "  $(CYAN)Grafana:$(NC)      http://localhost:3001 (admin/admin123)"
	@echo "  $(CYAN)Keycloak:$(NC)     http://localhost:8180 (admin/admin123)"
	@echo "  $(CYAN)Kafka UI:$(NC)     http://localhost:8089"
	@echo ""
	@echo "$(BOLD)Demo Users:$(NC) (password: $(YELLOW)password123$(NC))"
	@echo "  admin@brokage.com     - Full access"
	@echo "  broker1@brokage.com   - Broker role"
	@echo "  customer1@brokage.com - Customer role"
	@echo ""
	@echo "$(BOLD)Commands:$(NC)"
	@echo "  make status  - Check service health"
	@echo "  make logs    - View logs"
	@echo "  make stop    - Stop all services"
	@echo ""

# Alias for backward compatibility
up: start

# =============================================================================
# START WITHOUT FRONTEND (for local UI development)
# =============================================================================

start-backend: build-backend
	@echo ""
	@echo "$(BOLD)$(CYAN)Starting Brokage Trading Platform (Backend Only)...$(NC)"
	@echo ""
	@echo "$(YELLOW)[1/5]$(NC) Starting databases (PostgreSQL, MongoDB, Redis)..."
	@docker compose up -d postgres mongodb redis zookeeper
	@echo "      Waiting for databases to be ready..."
	@sleep 5
	@echo ""
	@echo "$(YELLOW)[2/5]$(NC) Starting Kafka..."
	@docker compose up -d kafka
	@echo "      Waiting for Kafka to be ready..."
	@sleep 10
	@docker compose up -d schema-registry kafka-init
	@echo ""
	@echo "$(YELLOW)[3/5]$(NC) Starting Keycloak (Identity Provider)..."
	@docker compose up -d keycloak lgtm traefik kafka-ui
	@echo "      Waiting for Keycloak to be ready..."
	@sleep 15
	@docker compose up -d keycloak-init
	@echo ""
	@echo "$(YELLOW)[4/5]$(NC) Starting backend microservices..."
	@docker compose up -d --build order-service asset-service customer-service order-processor notification-service audit-service web-api
	@echo ""
	@echo "$(YELLOW)[5/5]$(NC) Waiting for services to be healthy..."
	@sleep 10
	@echo ""
	@echo "$(BOLD)$(GREEN)=============================================$(NC)"
	@echo "$(BOLD)$(GREEN)  Backend Ready! Frontend skipped for dev.  $(NC)"
	@echo "$(BOLD)$(GREEN)=============================================$(NC)"
	@echo ""
	@echo "$(BOLD)Run frontend locally:$(NC)"
	@echo "  cd ../frontend/web-client && npm run dev"
	@echo ""
	@echo "$(BOLD)API Endpoints:$(NC)"
	@echo "  $(CYAN)Order Service:$(NC)    http://localhost:7081"
	@echo "  $(CYAN)Asset Service:$(NC)    http://localhost:7082"
	@echo "  $(CYAN)Customer Service:$(NC) http://localhost:7083"
	@echo "  $(CYAN)Keycloak:$(NC)         http://localhost:8180"
	@echo ""
	@echo "$(BOLD)Demo Users:$(NC) (password: $(YELLOW)password123$(NC))"
	@echo "  admin@brokage.com     - Full access"
	@echo "  broker1@brokage.com   - Broker role"
	@echo "  customer1@brokage.com - Customer role"
	@echo ""

# =============================================================================
# INFRASTRUCTURE COMMANDS
# =============================================================================

up-infra:
	@echo "$(BOLD)Starting infrastructure...$(NC)"
	@docker compose up -d postgres mongodb redis zookeeper
	@sleep 5
	@docker compose up -d kafka
	@sleep 10
	@docker compose up -d schema-registry keycloak lgtm traefik kafka-ui kafka-init
	@sleep 15
	@docker compose up -d keycloak-init
	@echo "$(GREEN)Infrastructure services started!$(NC)"
	@echo ""
	@echo "Run 'make up-apps' to start application services."

up-apps: build-backend
	@echo "$(BOLD)Starting application services...$(NC)"
	@docker compose up -d --build order-service asset-service customer-service order-processor notification-service audit-service web-api web-client
	@echo "$(GREEN)Application services started!$(NC)"

# =============================================================================
# LIFECYCLE COMMANDS
# =============================================================================

down:
	@echo "$(BOLD)Stopping all services...$(NC)"
	@docker compose down
	@echo "$(GREEN)All services stopped.$(NC)"

stop: down

restart: down start

restart-apps:
	@echo "$(BOLD)Restarting application services...$(NC)"
	@docker compose restart order-service asset-service customer-service order-processor notification-service audit-service web-api web-client

# =============================================================================
# LOGGING
# =============================================================================

logs:
	@docker compose logs -f

logs-apps:
	@docker compose logs -f order-service asset-service customer-service order-processor notification-service audit-service web-api web-client

logs-infra:
	@docker compose logs -f postgres mongodb redis kafka zookeeper keycloak

logs-kafka:
	@docker compose logs -f kafka zookeeper schema-registry kafka-init

logs-keycloak:
	@docker compose logs -f keycloak keycloak-init

logs-lgtm:
	@docker compose logs -f lgtm

logs-frontend:
	@docker compose logs -f web-client

logs-%:
	@docker compose logs -f $*

# =============================================================================
# STATUS & HEALTH CHECK
# =============================================================================

ps:
	@docker compose ps

status:
	@echo ""
	@echo "$(BOLD)$(CYAN)Service Health Check$(NC)"
	@echo "$(BOLD)=========================================$(NC)"
	@echo ""
	@echo "$(BOLD)Infrastructure:$(NC)"
	@printf "  PostgreSQL......... "
	@docker compose exec -T postgres pg_isready -U brokage > /dev/null 2>&1 && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  MongoDB............ "
	@docker compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" -u brokage -p brokage123 --authenticationDatabase admin --quiet > /dev/null 2>&1 && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Redis.............. "
	@docker compose exec -T redis redis-cli -a brokage123 ping > /dev/null 2>&1 && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Kafka.............. "
	@docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list > /dev/null 2>&1 && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Schema Registry.... "
	@curl -sf http://localhost:8091/subjects > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@echo ""
	@echo "$(BOLD)Platform Services:$(NC)"
	@printf "  Keycloak........... "
	@curl -sf http://localhost:8180/health/ready > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Grafana (LGTM)..... "
	@curl -sf http://localhost:3001/api/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Traefik............ "
	@curl -sf http://localhost:8090/api/rawdata > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@echo ""
	@echo "$(BOLD)Microservices:$(NC)"
	@printf "  Order Service...... "
	@curl -sf http://localhost:7081/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Asset Service...... "
	@curl -sf http://localhost:7082/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Customer Service... "
	@curl -sf http://localhost:7083/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Order Processor.... "
	@curl -sf http://localhost:7084/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Notification Svc... "
	@curl -sf http://localhost:7085/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Audit Service...... "
	@curl -sf http://localhost:7086/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@printf "  Web API............ "
	@curl -sf http://localhost:7087/actuator/health > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@echo ""
	@echo "$(BOLD)Frontend:$(NC)"
	@printf "  Web Client......... "
	@curl -sf http://localhost:4000 > /dev/null && echo "$(GREEN)OK$(NC)" || echo "$(RED)DOWN$(NC)"
	@echo ""

# =============================================================================
# CLEANUP
# =============================================================================

clean:
	@echo "$(BOLD)$(RED)Cleaning up everything...$(NC)"
	@echo "  Stopping all containers..."
	@docker compose down -v --remove-orphans 2>/dev/null || true
	@echo "  Removing brokage containers..."
	@for c in $$(docker ps -aq --filter "name=brokage" 2>/dev/null); do docker rm -f $$c 2>/dev/null; done || true
	@echo "  Removing brokage volumes..."
	@for v in $$(docker volume ls -q --filter "name=brokage" 2>/dev/null); do docker volume rm -f $$v 2>/dev/null; done || true
	@for v in $$(docker volume ls -q --filter "name=deployment_" 2>/dev/null); do docker volume rm -f $$v 2>/dev/null; done || true
	@echo "  Removing local images..."
	@for i in $$(docker images -q "deployment-*" 2>/dev/null); do docker rmi -f $$i 2>/dev/null; done || true
	@echo "$(GREEN)Clean complete!$(NC)"

# =============================================================================
# KAFKA UTILITIES
# =============================================================================

list-topics:
	@docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --list

describe-topics:
	@docker compose exec kafka kafka-topics --bootstrap-server kafka:29092 --describe

# =============================================================================
# DEFAULT
# =============================================================================

.DEFAULT_GOAL := help
