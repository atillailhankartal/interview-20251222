# Mikroservis Mimarisi

## Genel Bakis

Sistem, event-driven mikroservis mimarisi uzerine insa edilmistir. Her servis kendi veritabanina sahiptir ve servisler arasi iletisim Kafka uzerinden asenkron mesajlasma ile saglanir.

---

## Servis Sinirlari

```mermaid
flowchart TB
    subgraph External["Dis Dunya"]
        CLIENT[Istemciler]
    end

    subgraph Gateway["API Katmani"]
        TRAEFIK[Traefik API Gateway]
    end

    subgraph Core["Cekirdek Servisler"]
        ORDER[Order Service]
        ASSET[Asset Service]
        CUSTOMER[Customer Service]
    end

    subgraph Processing["Islem Servisleri"]
        PROCESSOR[Order Processor]
    end

    subgraph Support["Destek Servisleri"]
        NOTIFICATION[Notification Service]
        AUDIT[Audit Service]
    end

    subgraph Messaging["Mesajlasma"]
        KAFKA[Apache Kafka]
    end

    subgraph Auth["Kimlik Dogrulama"]
        KEYCLOAK[Keycloak]
    end

    CLIENT --> TRAEFIK
    TRAEFIK --> ORDER
    TRAEFIK --> ASSET
    TRAEFIK --> CUSTOMER
    TRAEFIK --> KEYCLOAK

    ORDER --> KAFKA
    ASSET --> KAFKA
    CUSTOMER --> KAFKA

    KAFKA --> PROCESSOR
    KAFKA --> NOTIFICATION
    KAFKA --> AUDIT

    PROCESSOR --> KAFKA
```

---

## Servis Detaylari

### Order Service

Emir yonetiminden sorumlu ana servistir.

```mermaid
flowchart LR
    subgraph OrderService["Order Service"]
        direction TB
        API[REST API]
        BL[Is Mantigi]
        REPO[Repository]
        OUTBOX[Outbox Table]
    end

    subgraph Operations["Islemler"]
        CREATE[Emir Olustur]
        LIST[Emirleri Listele]
        DELETE[Emir Iptal]
    end

    subgraph Events["Uretilen Event'ler"]
        E1[OrderCreatedEvent]
        E2[OrderCancelledEvent]
    end

    API --> BL
    BL --> REPO
    BL --> OUTBOX

    CREATE --> API
    LIST --> API
    DELETE --> API

    OUTBOX --> E1
    OUTBOX --> E2
```

### Asset Service

Varlik ve bakiye yonetiminden sorumludur.

```mermaid
flowchart LR
    subgraph AssetService["Asset Service"]
        direction TB
        API[REST API]
        BL[Is Mantigi]
        REPO[Repository]
    end

    subgraph Operations["Islemler"]
        QUERY[Bakiye Sorgula]
        RESERVE[Rezervasyon]
        RELEASE[Rezervasyon Coz]
        TRANSFER[Transfer]
    end

    subgraph Events["Uretilen Event'ler"]
        E1[AssetReservedEvent]
        E2[AssetReleasedEvent]
        E3[AssetTransferredEvent]
    end

    API --> BL
    BL --> REPO

    QUERY --> API
    RESERVE --> BL
    RELEASE --> BL
    TRANSFER --> BL

    BL --> E1
    BL --> E2
    BL --> E3
```

### Order Processor

Emir eslestirme ve saga yonetiminden sorumludur.

```mermaid
flowchart LR
    subgraph OrderProcessor["Order Processor"]
        direction TB
        CONSUMER[Kafka Consumer]
        ENGINE[Matching Engine]
        SAGA[Saga Manager]
    end

    subgraph Operations["Islemler"]
        MATCH[Emir Eslestir]
        SETTLE[Takas Yap]
        COMPENSATE[Telafi Et]
    end

    subgraph Events["Uretilen Event'ler"]
        E1[OrderMatchedEvent]
        E2[OrderFailedEvent]
        E3[SettlementCompletedEvent]
    end

    CONSUMER --> ENGINE
    CONSUMER --> SAGA

    MATCH --> ENGINE
    SETTLE --> SAGA
    COMPENSATE --> SAGA

    ENGINE --> E1
    ENGINE --> E2
    SAGA --> E3
```

### Notification Service

Bildirim yonetimi ve dagitimini yapar.

```mermaid
flowchart LR
    subgraph NotificationService["Notification Service"]
        direction TB
        CONSUMER[Kafka Consumer]
        PROCESSOR[Bildirim Isleyici]
        CHANNELS[Kanal Yoneticisi]
        STORE[MongoDB Store]
    end

    subgraph Channels["Kanallar"]
        EMAIL[E-posta]
        SMS[SMS]
        PUSH[Push Bildirim]
        WS[WebSocket]
    end

    subgraph Features["Ozellikler"]
        RETRY[Tekrar Deneme]
        HISTORY[Gecmis]
        PREFS[Tercihler]
    end

    CONSUMER --> PROCESSOR
    PROCESSOR --> CHANNELS
    PROCESSOR --> STORE

    CHANNELS --> EMAIL
    CHANNELS --> SMS
    CHANNELS --> PUSH
    CHANNELS --> WS

    STORE --> RETRY
    STORE --> HISTORY
    STORE --> PREFS
```

### Customer Service

Musteri ve tier yonetimini saglar.

```mermaid
flowchart LR
    subgraph CustomerService["Customer Service"]
        direction TB
        API[REST API]
        BL[Is Mantigi]
        REPO[Repository]
    end

    subgraph Operations["Islemler"]
        CRUD[Musteri CRUD]
        TIER[Tier Yonetimi]
        DEPOSIT[Para Yatir]
        WITHDRAW[Para Cek]
    end

    API --> BL
    BL --> REPO

    CRUD --> API
    TIER --> API
    DEPOSIT --> API
    WITHDRAW --> API
```

### Audit Service

Olay kaydi ve uyumluluk raporlamasi yapar.

```mermaid
flowchart LR
    subgraph AuditService["Audit Service"]
        direction TB
        CONSUMER[Kafka Consumer]
        PROCESSOR[Event Isleyici]
        STORE[MongoDB Store]
    end

    subgraph Features["Ozellikler"]
        LOGGING[Olay Kaydi]
        COMPLIANCE[Uyumluluk]
        ANALYTICS[Analitik]
    end

    CONSUMER --> PROCESSOR
    PROCESSOR --> STORE

    STORE --> LOGGING
    STORE --> COMPLIANCE
    STORE --> ANALYTICS
```

---

## Veritabani Izolasyonu

Her servis kendi veritabanina sahiptir (Database per Service pattern).

```mermaid
flowchart TB
    subgraph Services["Servisler"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph PostgreSQL["PostgreSQL Cluster"]
        DB1[(brokage_orders)]
        DB2[(brokage_assets)]
        DB3[(brokage_customers)]
        DB4[(brokage_processor)]
    end

    subgraph MongoDB["MongoDB"]
        M1[(notifications)]
        M2[(audit_logs)]
    end

    OS --> DB1
    AS --> DB2
    CS --> DB3
    OP --> DB4
    NS --> M1
    AU --> M2
```

**Onemli Kural:** Servisler birbirlerinin veritabanlarina dogrudan erismez. Tum iletisim Kafka uzerinden event'ler araciligiyla yapilir.

---

## Servisler Arasi Iletisim

```mermaid
sequenceDiagram
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant OP as Order Processor
    participant NS as Notification Service

    Note over OS,NS: Asenkron Event-Driven Iletisim

    OS->>K: OrderCreatedEvent
    K->>AS: OrderCreatedEvent
    AS->>AS: Bakiye Kontrol & Rezerve
    AS->>K: AssetReservedEvent

    K->>OS: AssetReservedEvent
    OS->>OS: Status = ORDER_CONFIRMED

    K->>OP: AssetReservedEvent
    OP->>OP: Eslestirme Bekle

    Note over OP: Admin Match Komutu

    OP->>K: OrderMatchedEvent
    K->>AS: Transfer Yap
    K->>NS: Bildirim Gonder
    K->>OS: Status Guncelle
```

---

## Servis Bagimliliklari

```mermaid
flowchart TB
    subgraph Infrastructure["Altyapi Katmani"]
        PG[PostgreSQL]
        MG[MongoDB]
        RD[Redis]
        KF[Kafka]
        ZK[Zookeeper]
        SR[Schema Registry]
    end

    subgraph Services["Uygulama Katmani"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph Gateway["Gateway Katmani"]
        TR[Traefik]
        KC[Keycloak]
    end

    subgraph Monitoring["Izleme Katmani"]
        LGTM[LGTM Stack]
    end

    %% Infrastructure dependencies
    KF --> ZK
    KF --> SR

    %% Service to Infrastructure
    OS --> PG
    OS --> KF
    OS --> RD

    AS --> PG
    AS --> KF

    CS --> PG
    CS --> KF

    OP --> PG
    OP --> KF

    NS --> MG
    NS --> KF

    AU --> MG
    AU --> KF

    %% Gateway dependencies
    TR --> OS
    TR --> AS
    TR --> CS
    TR --> KC

    %% Monitoring
    OS --> LGTM
    AS --> LGTM
    CS --> LGTM
    OP --> LGTM
    NS --> LGTM
    AU --> LGTM
```

---

## Baslangic Sirasi

Servislerin baslama sirasi kritik onem tasir:

```mermaid
flowchart LR
    subgraph Phase1["Faz 1: Altyapi"]
        PG[PostgreSQL]
        MG[MongoDB]
        RD[Redis]
        ZK[Zookeeper]
    end

    subgraph Phase2["Faz 2: Mesajlasma"]
        KF[Kafka]
        SR[Schema Registry]
    end

    subgraph Phase3["Faz 3: Kimlik"]
        KC[Keycloak]
    end

    subgraph Phase4["Faz 4: Cekirdek Servisler"]
        AS[Asset Service]
        CS[Customer Service]
        OS[Order Service]
    end

    subgraph Phase5["Faz 5: Islem Servisleri"]
        OP[Order Processor]
    end

    subgraph Phase6["Faz 6: Destek Servisleri"]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph Phase7["Faz 7: Gateway"]
        TR[Traefik]
        LGTM[LGTM]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
    Phase4 --> Phase5
    Phase5 --> Phase6
    Phase6 --> Phase7
```

---

## Olceklendirme Stratejisi

```mermaid
flowchart TB
    subgraph LoadBalancer["Load Balancer"]
        TR[Traefik]
    end

    subgraph OrderServiceCluster["Order Service Cluster"]
        OS1[Order Service 1]
        OS2[Order Service 2]
        OS3[Order Service 3]
    end

    subgraph AssetServiceCluster["Asset Service Cluster"]
        AS1[Asset Service 1]
        AS2[Asset Service 2]
    end

    subgraph KafkaCluster["Kafka Cluster"]
        K1[Broker 1]
        K2[Broker 2]
        K3[Broker 3]
    end

    TR --> OS1
    TR --> OS2
    TR --> OS3

    TR --> AS1
    TR --> AS2

    OS1 --> K1
    OS2 --> K2
    OS3 --> K3
```

**Olceklendirme Kurallari:**
- Kafka partition sayisi = maksimum consumer sayisi
- Consumer group icindeki her consumer bir partition tuketir
- ayni customerId her zaman ayni partition'a gider (siralama garantisi)

---

## Sonraki Adimlar

Daha detayli bilgi icin:
- **[Event-Driven Akislar](03-event-driven-akislar.md)** - Kafka, Outbox, Saga
- **[Veritabani Tasarimi](04-veritabani-tasarimi.md)** - Polyglot persistence
