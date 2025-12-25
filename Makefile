# =============================================================================
# Brokage Trading Platform
# =============================================================================
# Zero-Config, One-Click Demo System
#
# Quick Start:
#   make         - Show wizard & help
#   make start   - Start everything (one-click demo)
#   make stop    - Stop everything
# =============================================================================

.PHONY: help start start-backend stop restart status logs clean check wizard present
.SILENT: help wizard banner check-docker check-java

# Colors for terminal output
RED    := \033[0;31m
GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
PURPLE := \033[0;35m
CYAN   := \033[0;36m
WHITE  := \033[0;37m
BOLD   := \033[1m
NC     := \033[0m

# =============================================================================
# AUTO-DETECT JAVA
# =============================================================================
# Try common Java 17+ locations across platforms

# Check JAVA_HOME first
ifdef JAVA_HOME
    JAVA_PATH := $(JAVA_HOME)
else
    # macOS Homebrew locations
    JAVA_CANDIDATES := \
        /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
        /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home \
        /usr/lib/jvm/java-17-openjdk \
        /usr/lib/jvm/java-21-openjdk \
        /usr/lib/jvm/java-17-openjdk-amd64 \
        /usr/lib/jvm/java-21-openjdk-amd64 \
        /usr/lib/jvm/temurin-17-jdk \
        /usr/lib/jvm/temurin-21-jdk

    JAVA_PATH := $(shell for dir in $(JAVA_CANDIDATES); do \
        if [ -d "$$dir" ] && [ -x "$$dir/bin/java" ]; then \
            echo "$$dir"; \
            break; \
        fi; \
    done)
endif

# Fallback: try to find any Java 17+
ifeq ($(JAVA_PATH),)
    JAVA_PATH := $(shell \
        if command -v java >/dev/null 2>&1; then \
            java_version=$$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1); \
            if [ "$$java_version" -ge 17 ] 2>/dev/null; then \
                dirname $$(dirname $$(readlink -f $$(which java) 2>/dev/null || which java)); \
            fi; \
        fi)
endif

# =============================================================================
# DEFAULT TARGET - WIZARD
# =============================================================================

help: banner
	@echo ""
	@echo "$(BOLD)$(CYAN)Quick Start:$(NC)"
	@echo "  $(GREEN)make start$(NC)          - Start the complete system (one-click demo)"
	@echo "  $(GREEN)make start-backend$(NC)  - Start without frontend (for local UI dev)"
	@echo "  $(GREEN)make stop$(NC)           - Stop all services"
	@echo "  $(GREEN)make status$(NC)         - Check service health"
	@echo ""
	@echo "$(BOLD)$(CYAN)Development:$(NC)"
	@echo "  $(YELLOW)make dev$(NC)            - Start infrastructure + run frontend locally"
	@echo "  $(YELLOW)make build$(NC)          - Build backend services"
	@echo "  $(YELLOW)make test$(NC)           - Run backend tests"
	@echo "  $(YELLOW)make logs$(NC)           - View all logs"
	@echo ""
	@echo "$(BOLD)$(CYAN)Presentation:$(NC)"
	@echo "  $(RED)make present$(NC)        - 🎬 Live E2E demo (auto-starts if needed)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Utilities:$(NC)"
	@echo "  $(PURPLE)make check$(NC)          - Check prerequisites (Docker, Java, etc.)"
	@echo "  $(PURPLE)make clean$(NC)          - Remove all containers and data"
	@echo "  $(PURPLE)make restart$(NC)        - Restart all services"
	@echo ""
	@echo "$(BOLD)$(CYAN)URLs (after start):$(NC)"
	@echo "  Frontend:     $(BLUE)http://localhost:4000$(NC)"
	@echo "  API Gateway:  $(BLUE)http://localhost:4500$(NC)"
	@echo "  Grafana:      $(BLUE)http://localhost:3001$(NC) (admin/admin123)"
	@echo "  Keycloak:     $(BLUE)http://localhost:8180$(NC) (admin/admin123)"
	@echo "  Kafka UI:     $(BLUE)http://localhost:8089$(NC)"
	@echo "  Mailpit:      $(BLUE)http://localhost:8026$(NC) (Email testing)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Database Tools (for reviewers):$(NC)"
	@echo "  pgAdmin:      $(BLUE)http://localhost:5050$(NC) (admin@brokage.com/admin123)"
	@echo "  Mongo Express:$(BLUE)http://localhost:8027$(NC) (admin/admin123)"
	@echo ""
	@echo "$(BOLD)$(CYAN)API Documentation (Swagger):$(NC)"
	@echo "  Order Service:    $(BLUE)http://localhost:7081/swagger-ui.html$(NC)"
	@echo "  Asset Service:    $(BLUE)http://localhost:7082/swagger-ui.html$(NC)"
	@echo "  Customer Service: $(BLUE)http://localhost:7083/swagger-ui.html$(NC)"
	@echo "  Notification:     $(BLUE)http://localhost:7085/swagger-ui.html$(NC)"
	@echo "  Audit Service:    $(BLUE)http://localhost:7086/swagger-ui.html$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Demo Users:$(NC) (password: $(YELLOW)password123$(NC))"
	@echo "  $(GREEN)admin@brokage.com$(NC)     - ADMIN role"
	@echo "  $(GREEN)broker1@brokage.com$(NC)   - BROKER role"
	@echo "  $(GREEN)customer1@brokage.com$(NC) - CUSTOMER role"
	@echo ""

banner:
	@echo ""
	@echo "$(BOLD)$(BLUE)╔══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(BLUE)║$(NC)        $(BOLD)$(GREEN)BROKAGE TRADING PLATFORM$(NC)                             $(BOLD)$(BLUE)║$(NC)"
	@echo "$(BOLD)$(BLUE)║$(NC)        $(WHITE)Microservices Demo System$(NC)                            $(BOLD)$(BLUE)║$(NC)"
	@echo "$(BOLD)$(BLUE)╚══════════════════════════════════════════════════════════════╝$(NC)"

wizard: banner check
	@echo ""
	@echo "$(BOLD)$(GREEN)All prerequisites satisfied!$(NC)"
	@echo ""
	@echo "$(BOLD)Ready to start?$(NC)"
	@echo "  Run: $(CYAN)make start$(NC)"
	@echo ""

# =============================================================================
# PREREQUISITES CHECK
# =============================================================================

check: check-docker check-java check-ports
	@echo ""
	@echo "$(BOLD)$(GREEN)All checks passed!$(NC)"

check-docker:
	@echo ""
	@echo "$(BOLD)Checking prerequisites...$(NC)"
	@echo ""
	@printf "  Docker............. "
	@if command -v docker >/dev/null 2>&1; then \
		docker_version=$$(docker --version | cut -d' ' -f3 | tr -d ','); \
		echo "$(GREEN)OK$(NC) ($$docker_version)"; \
	else \
		echo "$(RED)NOT FOUND$(NC)"; \
		echo ""; \
		echo "$(RED)Docker is required. Please install Docker Desktop:$(NC)"; \
		echo "  https://www.docker.com/products/docker-desktop/"; \
		exit 1; \
	fi
	@printf "  Docker Compose..... "
	@if docker compose version >/dev/null 2>&1; then \
		compose_version=$$(docker compose version --short 2>/dev/null || echo "OK"); \
		echo "$(GREEN)OK$(NC) ($$compose_version)"; \
	else \
		echo "$(RED)NOT FOUND$(NC)"; \
		echo ""; \
		echo "$(RED)Docker Compose V2 is required.$(NC)"; \
		exit 1; \
	fi
	@printf "  Docker Running..... "
	@if docker info >/dev/null 2>&1; then \
		echo "$(GREEN)OK$(NC)"; \
	else \
		echo "$(RED)NOT RUNNING$(NC)"; \
		echo ""; \
		echo "$(RED)Please start Docker Desktop and try again.$(NC)"; \
		exit 1; \
	fi

check-java:
	@printf "  Java 17+........... "
	@if [ -n "$(JAVA_PATH)" ] && [ -x "$(JAVA_PATH)/bin/java" ]; then \
		java_version=$$($(JAVA_PATH)/bin/java -version 2>&1 | head -1 | cut -d'"' -f2); \
		echo "$(GREEN)OK$(NC) ($$java_version)"; \
	else \
		echo "$(YELLOW)NOT FOUND$(NC) (will use Docker for build)"; \
	fi

check-ports:
	@printf "  Port 4000.......... "
	@if lsof -i :4000 >/dev/null 2>&1; then \
		echo "$(YELLOW)IN USE$(NC) (may conflict with Frontend)"; \
	else \
		echo "$(GREEN)FREE$(NC)"; \
	fi
	@printf "  Port 4500.......... "
	@if lsof -i :4500 >/dev/null 2>&1; then \
		echo "$(YELLOW)IN USE$(NC) (may conflict with API Gateway)"; \
	else \
		echo "$(GREEN)FREE$(NC)"; \
	fi
	@printf "  Port 8180.......... "
	@if lsof -i :8180 >/dev/null 2>&1; then \
		echo "$(YELLOW)IN USE$(NC) (may conflict with Keycloak)"; \
	else \
		echo "$(GREEN)FREE$(NC)"; \
	fi
	@printf "  Port 8026.......... "
	@if lsof -i :8026 >/dev/null 2>&1; then \
		echo "$(YELLOW)IN USE$(NC) (may conflict with Mailpit)"; \
	else \
		echo "$(GREEN)FREE$(NC)"; \
	fi

# =============================================================================
# ONE-CLICK START
# =============================================================================

start: banner check
	@echo ""
	@echo "$(BOLD)$(CYAN)Starting Brokage Trading Platform...$(NC)"
	@echo ""
	@$(MAKE) -C deployment start JAVA_HOME="$(JAVA_PATH)"
	@echo ""
	@echo "$(BOLD)$(GREEN)System is ready!$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Available Services:$(NC)"
	@echo "  Frontend:     $(BLUE)http://localhost:4000$(NC)"
	@echo "  API Gateway:  $(BLUE)http://localhost:4500$(NC)"
	@echo "  Keycloak:     $(BLUE)http://localhost:8180$(NC)"
	@echo "  Kafka UI:     $(BLUE)http://localhost:8089$(NC)"
	@echo "  Grafana:      $(BLUE)http://localhost:3001$(NC)"
	@echo "  Mailpit:      $(BLUE)http://localhost:8026$(NC) $(YELLOW)(Email notifications)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Database Tools:$(NC)"
	@echo "  pgAdmin:      $(BLUE)http://localhost:5050$(NC) $(YELLOW)(PostgreSQL - admin@brokage.com/admin123)$(NC)"
	@echo "  Mongo Express:$(BLUE)http://localhost:8027$(NC) $(YELLOW)(MongoDB - admin/admin123)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)API Docs (Swagger):$(NC)"
	@echo "  $(BLUE)http://localhost:7081/swagger-ui.html$(NC) $(YELLOW)(Orders)$(NC)"
	@echo "  $(BLUE)http://localhost:7082/swagger-ui.html$(NC) $(YELLOW)(Assets)$(NC)"
	@echo "  $(BLUE)http://localhost:7083/swagger-ui.html$(NC) $(YELLOW)(Customers)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Tip:$(NC) View email notifications at Mailpit when orders are created/matched!"
	@echo ""

# Start without frontend (for local UI development)
start-backend: banner check
	@echo ""
	@echo "$(BOLD)$(CYAN)Starting Backend Only (Frontend skipped for local dev)...$(NC)"
	@echo ""
	@$(MAKE) -C deployment start-backend JAVA_HOME="$(JAVA_PATH)"
	@echo ""
	@echo "$(BOLD)$(GREEN)Backend is ready!$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Available Services:$(NC)"
	@echo "  API Gateway:  $(BLUE)http://localhost:4500$(NC)"
	@echo "  Keycloak:     $(BLUE)http://localhost:8180$(NC)"
	@echo "  Kafka UI:     $(BLUE)http://localhost:8089$(NC)"
	@echo "  Grafana:      $(BLUE)http://localhost:3001$(NC)"
	@echo "  Mailpit:      $(BLUE)http://localhost:8026$(NC) $(YELLOW)(Email notifications)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Database Tools:$(NC)"
	@echo "  pgAdmin:      $(BLUE)http://localhost:5050$(NC) $(YELLOW)(PostgreSQL - admin@brokage.com/admin123)$(NC)"
	@echo "  Mongo Express:$(BLUE)http://localhost:8027$(NC) $(YELLOW)(MongoDB - admin/admin123)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Tip:$(NC) Run frontend locally with: cd frontend/web-client && npm run dev"
	@echo ""

# Alias for start
up: start

# =============================================================================
# STOP & CLEANUP
# =============================================================================

stop:
	@echo "$(BOLD)Stopping all services...$(NC)"
	@$(MAKE) -C deployment down

down: stop

restart:
	@$(MAKE) -C deployment restart

clean:
	@echo "$(BOLD)$(RED)This will remove all containers, volumes, and data!$(NC)"
	@echo "Press Ctrl+C to cancel, or wait 5 seconds to continue..."
	@sleep 5
	@$(MAKE) -C deployment clean

# =============================================================================
# DEVELOPMENT MODE
# =============================================================================

dev: banner check
	@echo ""
	@echo "$(BOLD)$(CYAN)Starting Development Mode...$(NC)"
	@echo ""
	@echo "$(YELLOW)Step 1:$(NC) Starting infrastructure..."
	@$(MAKE) -C deployment up-infra
	@echo ""
	@echo "$(YELLOW)Step 2:$(NC) Starting backend services..."
	@$(MAKE) -C deployment up-apps JAVA_HOME="$(JAVA_PATH)"
	@echo ""
	@echo "$(GREEN)Infrastructure ready!$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Available Services:$(NC)"
	@echo "  API Gateway:  $(BLUE)http://localhost:4500$(NC)"
	@echo "  Keycloak:     $(BLUE)http://localhost:8180$(NC)"
	@echo "  Kafka UI:     $(BLUE)http://localhost:8089$(NC)"
	@echo "  Grafana:      $(BLUE)http://localhost:3001$(NC)"
	@echo "  Mailpit:      $(BLUE)http://localhost:8026$(NC) $(YELLOW)(Email notifications)$(NC)"
	@echo ""
	@echo "$(BOLD)$(CYAN)Database Tools:$(NC)"
	@echo "  pgAdmin:      $(BLUE)http://localhost:5050$(NC) $(YELLOW)(PostgreSQL)$(NC)"
	@echo "  Mongo Express:$(BLUE)http://localhost:8027$(NC) $(YELLOW)(MongoDB)$(NC)"
	@echo ""
	@echo "$(BOLD)Now run frontend locally:$(NC)"
	@echo "  cd frontend/web-client"
	@echo "  npm install"
	@echo "  npm run dev"
	@echo ""
	@echo "Frontend will be available at: $(BLUE)http://localhost:5173$(NC)"

dev-frontend:
	@echo "$(BOLD)Starting frontend development server...$(NC)"
	cd frontend/web-client && npm run dev

install-frontend:
	cd frontend/web-client && npm install

# =============================================================================
# BUILD & TEST
# =============================================================================

build:
	@echo "$(BOLD)Building backend services...$(NC)"
ifneq ($(JAVA_PATH),)
	cd backend && JAVA_HOME=$(JAVA_PATH) ./gradlew bootJar --parallel
else
	@echo "$(YELLOW)Java not found locally, using Docker for build...$(NC)"
	@$(MAKE) -C deployment build-backend-docker
endif

test:
	@echo "$(BOLD)Running backend tests...$(NC)"
ifneq ($(JAVA_PATH),)
	cd backend && JAVA_HOME=$(JAVA_PATH) ./gradlew test
else
	@echo "$(RED)Java 17+ required for running tests locally$(NC)"
	@exit 1
endif

# =============================================================================
# MONITORING & LOGS
# =============================================================================

status:
	@$(MAKE) -C deployment status

logs:
	@$(MAKE) -C deployment logs

logs-apps:
	@$(MAKE) -C deployment logs-apps

ps:
	@$(MAKE) -C deployment ps

# =============================================================================
# STRESS TESTS
# =============================================================================

stress-test:
	@echo "$(BOLD)Running stress tests...$(NC)"
	cd stress-tests && ./k6/run.sh

# =============================================================================
# PRESENTATION MODE
# =============================================================================

present:
	@echo "$(BOLD)$(CYAN)Starting Presentation Mode...$(NC)"
	@./scripts/present.sh

# =============================================================================
# QUICK REFERENCE
# =============================================================================

.DEFAULT_GOAL := help
