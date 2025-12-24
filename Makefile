# =============================================================================
# Brokage System - Root Makefile
# =============================================================================
# One-click demo: make up
# =============================================================================

.PHONY: help up down restart logs ps clean build test status

# Default target
help:
	@$(MAKE) -C deployment help

# =============================================================================
# ONE-CLICK DEMO
# =============================================================================
up:
	@$(MAKE) -C deployment up

# =============================================================================
# DOCKER OPERATIONS
# =============================================================================
up-infra:
	@$(MAKE) -C deployment up-infra

up-apps:
	@$(MAKE) -C deployment up-apps

down:
	@$(MAKE) -C deployment down

restart:
	@$(MAKE) -C deployment restart

restart-apps:
	@$(MAKE) -C deployment restart-apps

logs:
	@$(MAKE) -C deployment logs

logs-apps:
	@$(MAKE) -C deployment logs-apps

logs-%:
	@$(MAKE) -C deployment $@

ps:
	@$(MAKE) -C deployment ps

status:
	@$(MAKE) -C deployment status

clean:
	@$(MAKE) -C deployment clean

list-topics:
	@$(MAKE) -C deployment list-topics

describe-topics:
	@$(MAKE) -C deployment describe-topics

# =============================================================================
# BACKEND BUILD
# =============================================================================
JAVA_HOME_PATH := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

build:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew bootJar --parallel

test:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew test

compile:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew compileJava

# =============================================================================
# FRONTEND DEVELOPMENT
# =============================================================================
dev-frontend:
	@echo "Starting frontend development server..."
	@echo "Make sure infrastructure is running: make up-infra"
	@echo ""
	cd frontend/web-client && npm run dev

install-frontend:
	cd frontend/web-client && npm install

build-frontend:
	cd frontend/web-client && npm run build

logs-frontend:
	@$(MAKE) -C deployment logs-frontend
