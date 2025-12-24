# Sistem Genel Bakis

## Proje Amaci

Bu proje, bir **araci kurum (brokerage firm)** icin backend API gelistirmektedir. Sistem, kurum calisanlarinin musteriler adina hisse senedi alim-satim emirleri vermesini ve yonetmesini saglar.

---

## Genel Mimari

```mermaid
flowchart TB
    subgraph Clients["Istemciler"]
        WEB[Web Tarayici]
        MOBILE[Mobil Uygulama]
        API_CLIENT[API Istemcisi]
    end

    subgraph Gateway["API Gateway Katmani"]
        TRAEFIK[Traefik Gateway]
        subgraph GW_Features["Gateway Ozellikleri"]
            RL[Rate Limiting]
            JWT[JWT Dogrulama]
            CB[Circuit Breaker]
        end
    end

    subgraph Services["Mikroservisler"]
        ORDER[Order Service]
        ASSET[Asset Service]
        CUSTOMER[Customer Service]
        PROCESSOR[Order Processor]
        NOTIFICATION[Notification Service]
        AUDIT[Audit Service]
    end

    subgraph Messaging["Mesajlasma Altyapisi"]
        KAFKA[Apache Kafka]
    end

    subgraph Data["Veri Katmani"]
        PG[(PostgreSQL)]
        MONGO[(MongoDB)]
        REDIS[(Redis Cache)]
    end

    subgraph Auth["Kimlik Dogrulama"]
        KEYCLOAK[Keycloak]
    end

    subgraph Monitoring["Izleme"]
        LGTM[LGTM Stack]
    end

    WEB --> TRAEFIK
    MOBILE --> TRAEFIK
    API_CLIENT --> TRAEFIK

    TRAEFIK --> RL
    TRAEFIK --> JWT
    TRAEFIK --> CB

    TRAEFIK --> ORDER
    TRAEFIK --> ASSET
    TRAEFIK --> CUSTOMER
    TRAEFIK --> KEYCLOAK

    ORDER --> KAFKA
    ASSET --> KAFKA
    PROCESSOR --> KAFKA

    KAFKA --> PROCESSOR
    KAFKA --> NOTIFICATION
    KAFKA --> AUDIT

    ORDER --> PG
    ASSET --> PG
    CUSTOMER --> PG
    PROCESSOR --> PG
    NOTIFICATION --> MONGO
    AUDIT --> MONGO

    ORDER --> REDIS
    ASSET --> REDIS

    JWT --> KEYCLOAK

    Services --> LGTM
```

---

## Temel Kavramlar

### Emir (Order) Nedir?

Bir musteri, hisse senedi almak veya satmak istediginde bir **emir** olusturur. Emir su bilgileri icerir:

```mermaid
erDiagram
    ORDER {
        Long id PK
        Long customerId FK
        String assetName
        String orderSide
        BigDecimal size
        BigDecimal price
        String status
        DateTime createDate
    }

    ORDER ||--o{ STATUS : has

    STATUS {
        String PENDING
        String MATCHED
        String CANCELED
        String REJECTED
    }
```

### Varlik (Asset) Nedir?

Musterinin sahip oldugu degerler **varlik** olarak tutulur. TRY (Turk Lirasi) da bir varliktir.

```mermaid
erDiagram
    ASSET {
        Long id PK
        Long customerId FK
        String assetName
        BigDecimal size
        BigDecimal usableSize
    }
```

**Onemli:** `size` toplam miktari, `usableSize` kullanilabilir (bloke edilmemis) miktari gosterir.

---

## Is Akis Ozeti

### Alim (BUY) Emri Akisi

```mermaid
sequenceDiagram
    participant M as Musteri
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant P as Order Processor

    M->>API: POST /api/orders (BUY)
    API->>API: JWT Dogrulama
    API->>OS: Emir Olustur
    OS->>OS: Emir Kaydet (PENDING_RESERVATION)
    OS->>K: OrderCreatedEvent
    K->>AS: Bakiye Rezerve Et
    AS->>AS: TRY usableSize Azalt
    AS->>K: AssetReservedEvent
    K->>OS: Status Guncelle (ORDER_CONFIRMED)
    OS->>M: Emir Olusturuldu

    Note over M,P: Daha sonra Admin eslestirdiginde...

    P->>OS: Match Emri
    OS->>K: OrderMatchedEvent
    K->>AS: TRY size Azalt, Hisse Ekle
    AS->>K: SettlementCompletedEvent
```

### Satis (SELL) Emri Akisi

```mermaid
sequenceDiagram
    participant M as Musteri
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant P as Order Processor

    M->>API: POST /api/orders (SELL)
    API->>API: JWT Dogrulama
    API->>OS: Emir Olustur
    OS->>OS: Emir Kaydet (PENDING_RESERVATION)
    OS->>K: OrderCreatedEvent
    K->>AS: Hisse Rezerve Et
    AS->>AS: Hisse usableSize Azalt
    AS->>K: AssetReservedEvent
    K->>OS: Status Guncelle (ORDER_CONFIRMED)
    OS->>M: Emir Olusturuldu

    Note over M,P: Daha sonra Admin eslestirdiginde...

    P->>OS: Match Emri
    OS->>K: OrderMatchedEvent
    K->>AS: Hisse size Azalt, TRY Ekle
    AS->>K: SettlementCompletedEvent
```

### Emir Iptal Akisi

```mermaid
sequenceDiagram
    participant M as Musteri
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service

    M->>API: DELETE /api/orders/{id}
    API->>API: JWT + Yetki Kontrolu
    API->>OS: Emir Iptal
    OS->>OS: Status = PENDING mi?

    alt Emir PENDING Degil
        OS->>M: Hata: Sadece PENDING emirler iptal edilebilir
    else Emir PENDING
        OS->>OS: Status = CANCELED
        OS->>K: OrderCancelledEvent
        K->>AS: Bloke Coz
        AS->>AS: usableSize Artir
        AS->>K: AssetReleasedEvent
        OS->>M: Emir Iptal Edildi
    end
```

---

## Servis Sorumlulukları

```mermaid
flowchart LR
    subgraph OrderService["Order Service"]
        O1[Emir Olusturma]
        O2[Emir Listeleme]
        O3[Emir Iptal]
        O4[Outbox Pattern]
    end

    subgraph AssetService["Asset Service"]
        A1[Bakiye Sorgulama]
        A2[Bakiye Rezervasyonu]
        A3[Bakiye Transferi]
        A4[Bloke Cozme]
    end

    subgraph CustomerService["Customer Service"]
        C1[Musteri Yonetimi]
        C2[Tier Yonetimi]
        C3[Para Yatirma]
        C4[Para Cekme]
    end

    subgraph OrderProcessor["Order Processor"]
        P1[Emir Eslestirme]
        P2[Takas Islemi]
        P3[Saga Yonetimi]
    end

    subgraph NotificationService["Notification Service"]
        N1[E-posta Bildirimi]
        N2[SMS Bildirimi]
        N3[Push Bildirimi]
        N4[WebSocket]
    end

    subgraph AuditService["Audit Service"]
        AU1[Olay Kaydi]
        AU2[Uyumluluk Raporlari]
        AU3[Analitik]
    end
```

---

## Musteri Tier Sistemi (Task İsterlerinde yok ama neden bir ürün olarak sunmayalım diye kendim ekledim.)

Sistem, musterileri tier'lara ayirarak farkli hizmet seviyeleri sunar:

```mermaid
flowchart TB
    subgraph TierSystem["Musteri Tier Sistemi"]
        VIP["VIP Tier<br/>En Yuksek Oncelik<br/>1000 req/dk"]
        PREMIUM["Premium Tier<br/>Orta Oncelik<br/>500 req/dk"]
        STANDARD["Standard Tier<br/>Normal Oncelik<br/>100 req/dk"]
    end

    subgraph Benefits["Avantajlar"]
        B1[Islem Onceligi]
        B2[Rate Limit]
        B3[Ozel Destek]
    end

    VIP --> B1
    VIP --> B2
    VIP --> B3

    PREMIUM --> B1
    PREMIUM --> B2

    STANDARD --> B1
```

**Is Mantigi:** Emir eslestirilirken once tier'a, sonra fiyata, en son zamana gore siralama yapilir.

---

## Yetkilendirme Matrisi

```mermaid
flowchart TB
    subgraph Roles["Roller"]
        ADMIN[Admin]
        CUSTOMER[Customer]
    end

    subgraph Endpoints["Endpoint'ler"]
        CREATE[POST /api/orders]
        LIST[GET /api/orders]
        DELETE[DELETE /api/orders]
        MATCH[POST /api/orders/.../match]
        ASSETS[GET /api/assets]
    end

    ADMIN -->|Tam Erisim| CREATE
    ADMIN -->|Tam Erisim| LIST
    ADMIN -->|Tam Erisim| DELETE
    ADMIN -->|Ozel Yetki| MATCH
    ADMIN -->|Tam Erisim| ASSETS

    CUSTOMER -->|Kendi Verileri| CREATE
    CUSTOMER -->|Kendi Verileri| LIST
    CUSTOMER -->|Kendi Emri + PENDING| DELETE
    CUSTOMER -.->|Erisim Yok| MATCH
    CUSTOMER -->|Kendi Verileri| ASSETS

    style MATCH fill:#f96,stroke:#333
```

---

## Teknoloji Yigini

```mermaid
mindmap
    root((Brokage System))
        Backend
            Spring Boot 3
            Java 21
            Gradle
        Veritabani
            PostgreSQL
                Order DB
                Asset DB
                Customer DB
            MongoDB
                Notifications
                Audit Logs
            Redis
                Cache
                Idempotency
        Mesajlasma
            Apache Kafka
            Schema Registry
            Avro
        Guvenlik
            Keycloak
            JWT/OAuth2
            mTLS
        API Gateway
            Traefik
            Rate Limiting
            Circuit Breaker
        Izleme
            Grafana
            Loki
            Tempo
            Prometheus
        Test
            JUnit 5
            Testcontainers
            K6 Stress Test
```

---

## Sonraki Adimlar

Sistemin daha detayli mimarisini anlamak icin asagidaki dokumanlara bakabilirsiniz:

1. **[Mikroservis Mimarisi](02-mikroservis-mimarisi.md)** - Servislerin detayli yapisi
2. **[Event-Driven Akislar](03-event-driven-akislar.md)** - Kafka ve Saga pattern
3. **[Veritabani Tasarimi](04-veritabani-tasarimi.md)** - Polyglot persistence
4. **[API Gateway ve Guvenlik](05-api-gateway-guvenlik.md)** - Traefik ve Keycloak
5. **[Monitoring ve Observability](06-monitoring-observability.md)** - LGTM Stack
