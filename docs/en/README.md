# Brokage System Documentation

Technical documentation for the brokerage firm backend API system.

## Documents

| # | Document | Description |
|---|----------|-------------|
| 1 | [System Overview](01-system-overview.md) | Architecture, concepts and business flows |
| 2 | [Microservices Architecture](02-microservices-architecture.md) | Service boundaries and communication |
| 3 | [Event-Driven Flows](03-event-driven-flows.md) | Kafka, Saga, Idempotency |
| 4 | [Database Design](04-database-design.md) | Polyglot persistence |
| 5 | [API Gateway and Security](05-api-gateway-security.md) | Traefik, Keycloak, JWT |
| 6 | [Monitoring and Observability](06-monitoring-observability.md) | LGTM Stack |

## Technology Stack

- **Backend:** Spring Boot 3, Java 21
- **Database:** PostgreSQL, MongoDB, Redis
- **Messaging:** Apache Kafka, Avro
- **API Gateway:** Traefik
- **Authentication:** Keycloak (JWT/OAuth2)
- **Monitoring:** Grafana, Loki, Tempo, Mimir

## Other Languages

- [Turkish Documentation](../tr/README.md)
