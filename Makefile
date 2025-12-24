# =============================================================================
# Brokage System - Root Makefile
# =============================================================================

.PHONY: help up down restart logs ps clean build test

# Default target
help:
	@echo "Brokage System - Development Commands"
	@echo ""
	@echo "Docker Operations:"
	@echo "  make up              - Start all services"
	@echo "  make up-infra        - Start infrastructure only"
	@echo "  make down            - Stop all services"
	@echo "  make restart         - Restart all services"
	@echo "  make logs            - View all logs"
	@echo "  make ps              - List running services"
	@echo "  make clean           - Stop and remove volumes"
	@echo ""
	@echo "Build Operations:"
	@echo "  make build           - Build all backend services"
	@echo "  make test            - Run all tests"
	@echo "  make compile         - Compile Java sources"
	@echo ""
	@echo "URLs:"
	@echo "  Grafana:     http://localhost:3001 (admin/admin123)"
	@echo "  Keycloak:    http://localhost:8180 (admin/admin123)"
	@echo "  Traefik:     http://localhost:8090"
	@echo "  Kafka UI:    http://localhost:8083"

# =============================================================================
# Docker Operations (delegated to deployment/)
# =============================================================================
up:
	@$(MAKE) -C deployment up

up-infra:
	@$(MAKE) -C deployment up-infra

down:
	@$(MAKE) -C deployment down

restart:
	@$(MAKE) -C deployment restart

logs:
	@$(MAKE) -C deployment logs

logs-%:
	@$(MAKE) -C deployment $@

ps:
	@$(MAKE) -C deployment ps

clean:
	@$(MAKE) -C deployment clean

create-topics:
	@$(MAKE) -C deployment create-topics

list-topics:
	@$(MAKE) -C deployment list-topics

test-connections:
	@$(MAKE) -C deployment test-connections

# =============================================================================
# Backend Build Operations
# =============================================================================
JAVA_HOME_PATH := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

build:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew build

test:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew test

compile:
	cd backend && JAVA_HOME=$(JAVA_HOME_PATH) ./gradlew compileJava
