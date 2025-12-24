# Brokage Sistemi Dokumantasyonu

Araci kurum backend API sisteminin teknik dokumantasyonu.

## Dokumanlar

| # | Dokuman | Aciklama |
|---|---------|----------|
| 1 | [Sistem Genel Bakis](01-sistem-genel-bakis.md) | Mimari, kavramlar ve is akislari |
| 2 | [Mikroservis Mimarisi](02-mikroservis-mimarisi.md) | Servis sinirlari ve iletisim |
| 3 | [Event-Driven Akislar](03-event-driven-akislar.md) | Kafka, Saga, Idempotency |
| 4 | [Veritabani Tasarimi](04-veritabani-tasarimi.md) | Polyglot persistence |
| 5 | [API Gateway ve Guvenlik](05-api-gateway-guvenlik.md) | Traefik, Keycloak, JWT |
| 6 | [Monitoring ve Observability](06-monitoring-observability.md) | LGTM Stack |

## Teknoloji Yigini

- **Backend:** Spring Boot 3, Java 21
- **Veritabani:** PostgreSQL, MongoDB, Redis
- **Mesajlasma:** Apache Kafka, Avro
- **API Gateway:** Traefik
- **Kimlik Dogrulama:** Keycloak (JWT/OAuth2)
- **Monitoring:** Grafana, Loki, Tempo, Mimir

## Diger Diller

- [English Documentation](../en/README.md)
