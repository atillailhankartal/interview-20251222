# API Gateway ve Guvenlik

## Genel Bakis

Sistem, API Gateway olarak **Traefik** kullanir. Kimlik dogrulama ve yetkilendirme icin **Keycloak** entegre edilmistir.

---

## API Gateway Mimarisi

```mermaid
flowchart TB
    subgraph Internet["Internet"]
        CLIENT[Istemciler]
    end

    subgraph Gateway["Traefik API Gateway"]
        ENTRY["Entrypoint 80/443"]

        subgraph Middlewares["Middleware Zinciri"]
            RL[Rate Limiter]
            SEC[Security Headers]
            CORS[CORS]
            JWT[JWT Dogrulama]
            CB[Circuit Breaker]
        end

        subgraph Routers["Yonlendiriciler"]
            R1["api/orders"]
            R2["api/assets"]
            R3["api/customers"]
            R4["api/notifications"]
            R5["auth"]
        end
    end

    subgraph Services["Servisler"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        NS[Notification Service]
        KC[Keycloak]
    end

    CLIENT --> ENTRY
    ENTRY --> RL
    RL --> SEC
    SEC --> CORS
    CORS --> JWT
    JWT --> CB

    CB --> R1
    CB --> R2
    CB --> R3
    CB --> R4
    CB --> R5

    R1 --> OS
    R2 --> AS
    R3 --> CS
    R4 --> NS
    R5 --> KC
```

---

## Guvenlik Middleware'leri

### Rate Limiting

Asiri istekleri engelleyerek servisleri korur.

```mermaid
flowchart LR
    subgraph Requests["Gelen Istekler"]
        R1[Istek 1]
        R2[Istek 2]
        R3[Istek 3]
        RN[Istek N...]
    end

    subgraph RateLimiter["Rate Limiter"]
        CHECK{Limit Asildi mi?}
        ALLOW[Izin Ver]
        DENY[429 Too Many Requests]
    end

    subgraph Service["Servis"]
        SVC[Uygulama]
    end

    Requests --> CHECK
    CHECK -->|Hayir| ALLOW
    CHECK -->|Evet| DENY
    ALLOW --> SVC
```

**Limit Turleri:**

```mermaid
flowchart TB
    subgraph Limits["Rate Limit Politikalari"]
        GLOBAL["Global<br/>100 req/sn"]
        ORDERS["Orders API<br/>10 req/sn"]
        AUTH["Auth API<br/>5 req/sn"]
    end

    subgraph Reasons["Gerekce"]
        G1[Genel Koruma]
        G2[Islem Yonetimi]
        G3[Brute-force Engeli]
    end

    GLOBAL --> G1
    ORDERS --> G2
    AUTH --> G3
```

### Tier Bazli Rate Limiting

Musteri tier'lari uygulama katmaninda farkli rate limit'lere sahiptir:

```mermaid
flowchart TB
    subgraph Gateway["Traefik Gateway"]
        GLOBAL_RL["Global Rate Limit<br/>100 req/sn temel"]
    end

    subgraph Application["Uygulama Katmani"]
        subgraph TierLimits["Tier Bazli Limitler (dakika basina)"]
            VIP["VIP Tier<br/>1000 req/dk"]
            PREMIUM["Premium Tier<br/>500 req/dk"]
            STANDARD["Standard Tier<br/>100 req/dk"]
        end
    end

    subgraph Headers["Yanit Basliklari"]
        H1["X-RateLimit-Limit"]
        H2["X-RateLimit-Remaining"]
        H3["X-RateLimit-Reset"]
        H4["X-Customer-Tier"]
    end

    Gateway --> Application
    TierLimits --> Headers
```

**Implementasyon:**
- Gateway temel rate limiting uygular (100 req/sn global)
- Uygulama katmani (`TierRateLimitFilter`) tier-bazli limit uygular
- Tier, JWT `customer_tier` claim'inden alinir
- Rate limit basliklari yanita eklenir

### Security Headers

```mermaid
flowchart LR
    subgraph Headers["Guvenlik Basliklari"]
        H1["X-XSS-Protection<br/>XSS Korumasi"]
        H2["X-Content-Type-Options<br/>MIME Sniffing Engeli"]
        H3["X-Frame-Options<br/>Clickjacking Korumasi"]
        H4["Strict-Transport-Security<br/>HTTPS Zorunlulugu"]
        H5["Content-Security-Policy<br/>Icerik Politikasi"]
    end

    subgraph Protection["Koruma"]
        P1[Script Enjeksiyonu]
        P2[Icerik Tipi Sahteciligi]
        P3[Iframe Saldirisi]
        P4[Sifresiz Baglanti]
        P5[Kaynak Kisitlamasi]
    end

    H1 --> P1
    H2 --> P2
    H3 --> P3
    H4 --> P4
    H5 --> P5
```

### CORS (Cross-Origin Resource Sharing)

```mermaid
sequenceDiagram
    participant B as Tarayici
    participant T as Traefik
    participant S as Servis

    Note over B,S: Preflight Request

    B->>T: OPTIONS /api/orders
    T->>T: CORS Kontrolu

    alt Origin Izinli
        T-->>B: 200 OK<br/>Access-Control-Allow-Origin: *
        B->>T: POST /api/orders
        T->>S: Forward
        S-->>T: Response
        T-->>B: Response + CORS Headers
    else Origin Izinsiz
        T-->>B: 403 Forbidden
    end
```

### Circuit Breaker

```mermaid
stateDiagram-v2
    [*] --> CLOSED: Baslangic

    state CLOSED {
        [*] --> Monitoring
        Monitoring --> Monitoring: Basarili Istek
        Monitoring --> Threshold: Hata Sayisi Artiyor
    }

    CLOSED --> OPEN: Hata Orani > %30

    state OPEN {
        [*] --> Blocking
        Blocking --> Blocking: Istek Engellendi
    }

    OPEN --> HALF_OPEN: 30 sn Bekleme

    state HALF_OPEN {
        [*] --> Testing
        Testing --> Success: Test Basarili
        Testing --> Failure: Test Basarisiz
    }

    HALF_OPEN --> CLOSED: Test Basarili
    HALF_OPEN --> OPEN: Test Basarisiz
```

---

## Kimlik Dogrulama

### JWT Akisi - Login

```mermaid
sequenceDiagram
    participant C as Client
    participant K as Keycloak
    participant DB as User Store

    C->>K: POST /auth/realms/brokage/protocol/openid-connect/token
    Note right of C: grant_type=password<br/>username=john<br/>password=secret<br/>client_id=brokage-app

    K->>DB: Kullanici Dogrula
    DB-->>K: User + Roles

    K->>K: JWT Olustur
    Note right of K: Header: alg=RS256<br/>Payload: sub, roles, exp<br/>Sign with Private Key

    K-->>C: Access Token + Refresh Token
```

### JWT Akisi - API Erisimi (Role-Based)

```mermaid
sequenceDiagram
    participant C as Client
    participant T as Traefik
    participant S as Order Service
    participant SEC as Spring Security

    C->>T: DELETE /api/orders/123<br/>Authorization: Bearer eyJhbG...

    T->>T: JWT Format Kontrolu
    T->>T: Signature Dogrulama

    alt Token Gecersiz
        T-->>C: 401 Unauthorized
    end

    T->>S: Forward Request<br/>+ Original JWT Header

    S->>SEC: JWT Token Coz
    SEC->>SEC: Claims Cikart
    Note right of SEC: sub: user-123<br/>roles: CUSTOMER<br/>customerId: 456

    SEC->>SEC: Yetki Kontrolu
    Note right of SEC: DELETE /orders/{id}<br/>Gerekli: ADMIN veya<br/>Kendi emri + PENDING

    alt ADMIN rolu var
        SEC-->>S: Yetkili
        S->>S: Emir Iptal Et
        S-->>C: 200 OK
    else CUSTOMER + Kendi Emri + PENDING
        SEC-->>S: Yetkili
        S->>S: Emir Iptal Et
        S-->>C: 200 OK
    else Yetkisiz
        SEC-->>C: 403 Forbidden
    end
```

### Match Islemi - ADMIN ve BROKER

```mermaid
sequenceDiagram
    participant C as Client
    participant T as Traefik
    participant S as Order Service
    participant SEC as Spring Security

    C->>T: POST /api/orders/123/match<br/>Authorization: Bearer eyJhbG...

    T->>S: Forward Request

    S->>SEC: JWT Token Coz
    SEC->>SEC: Rol Kontrolu
    Note right of SEC: Gerekli Rol: ADMIN veya BROKER

    alt roles contains ADMIN
        SEC-->>S: Yetkili
        S->>S: Emri Eslestir
        S-->>C: 200 OK - Matched
    else roles contains BROKER
        SEC->>SEC: Alt Musteri Kontrolu
        alt Emir alt musteriye ait
            SEC-->>S: Yetkili
            S->>S: Emri Eslestir
            S-->>C: 200 OK - Matched
        else Alt musteri degil
            SEC-->>C: 403 Forbidden
        end
    else CUSTOMER veya Baska
        SEC-->>C: 403 Forbidden<br/>Admin veya Broker yetkisi gerekli
    end
```

### JWT Token Yapisi

```mermaid
flowchart TB
    subgraph Token["JWT Token Yapisi"]
        subgraph Header["Header"]
            ALG["alg: RS256"]
            TYP["typ: JWT"]
        end

        subgraph Payload["Payload - Claims"]
            SUB["sub: user-uuid"]
            NAME["preferred_username: john"]
            ROLES["realm_access.roles"]
            USER_ID["userId: 456"]
            EXP["exp: 1704067200"]
        end

        subgraph Signature["Signature"]
            SIG["RSASHA256 signed"]
        end
    end

    subgraph Note["NOT"]
        N1["managedCustomerIds token'da YOK"]
        N2["DB + Redis Cache'den alinir"]
    end

    subgraph RoleValues["Rol Degerleri"]
        R1["ADMIN - Tum musterilerde yetkili"]
        R2["BROKER - Alt musterileri DB'den"]
        R3["CUSTOMER - userId = customerId"]
    end

    ROLES --> R1
    ROLES --> R2
    ROLES --> R3
```

### Yetki Kontrolu - DB + Redis Cache

```mermaid
flowchart TB
    subgraph Request["Gelen Istek"]
        REQ["GET /api/orders?customerId=201<br/>JWT: userId=101, role=BROKER"]
    end

    subgraph AuthFlow["Yetkilendirme Akisi"]
        EXTRACT[JWT'den userId ve role cikart]
        CHECK_ROLE{Role?}

        subgraph AdminPath["ADMIN Path"]
            ADMIN_OK[Direkt Erisim]
        end

        subgraph BrokerPath["BROKER Path"]
            CACHE_CHECK{Redis Cache?}
            CACHE_HIT[Cache Hit]
            CACHE_MISS[Cache Miss]
            DB_QUERY[PostgreSQL Sorgu]
            CACHE_SET[Redis'e Yaz]
            PERM_CHECK{customerId yetkili mi?}
        end

        subgraph CustomerPath["CUSTOMER Path"]
            SELF_CHECK{userId == customerId?}
        end
    end

    subgraph Result["Sonuc"]
        OK[200 OK]
        FORBIDDEN[403 Forbidden]
    end

    REQ --> EXTRACT
    EXTRACT --> CHECK_ROLE

    CHECK_ROLE -->|ADMIN| ADMIN_OK
    CHECK_ROLE -->|BROKER| CACHE_CHECK
    CHECK_ROLE -->|CUSTOMER| SELF_CHECK

    ADMIN_OK --> OK

    CACHE_CHECK -->|Hit| CACHE_HIT
    CACHE_CHECK -->|Miss| CACHE_MISS
    CACHE_HIT --> PERM_CHECK
    CACHE_MISS --> DB_QUERY
    DB_QUERY --> CACHE_SET
    CACHE_SET --> PERM_CHECK
    PERM_CHECK -->|Evet| OK
    PERM_CHECK -->|Hayir| FORBIDDEN

    SELF_CHECK -->|Evet| OK
    SELF_CHECK -->|Hayir| FORBIDDEN

    style OK fill:#6f6
    style FORBIDDEN fill:#f66
```

### Redis Cache Yapisi

```mermaid
flowchart LR
    subgraph Redis["Redis Cache"]
        subgraph Keys["Key Yapisi"]
            K1["broker:101:customers"]
            K2["broker:102:customers"]
            K3["broker:103:customers"]
        end

        subgraph Values["Value - Set"]
            V1["201, 202"]
            V2["201, 203"]
            V3["202, 203"]
        end

        subgraph TTL["TTL"]
            T1["5 dakika"]
        end
    end

    K1 --> V1
    K2 --> V2
    K3 --> V3

    Keys --> TTL
```

### Cache Invalidation

```mermaid
sequenceDiagram
    participant A as Admin
    participant API as Customer Service
    participant DB as PostgreSQL
    participant R as Redis
    participant K as Kafka

    A->>API: POST /broker/101/customers<br/>Add customerId: 204

    API->>DB: INSERT broker_customer

    API->>R: DELETE broker:101:customers
    Note right of R: Cache Invalidate

    API->>K: BrokerCustomerUpdatedEvent

    API-->>A: 200 OK

    Note over R: Sonraki istek DB'den<br/>yeni liste alir ve cache'ler
```

### 3 Seviyeli Yetkilendirme Modeli

```mermaid
flowchart TB
    subgraph Hierarchy["Yetki Hiyerarsisi"]
        ADMIN["ADMIN<br/>Sistem Yoneticisi"]
        BROKER["BROKER<br/>Araci Kurum Calisani"]
        CUSTOMER["CUSTOMER<br/>Musteri"]
    end

    subgraph Scope["Erisim Kapsamı"]
        S1["Tum Musteriler<br/>Tum Islemler"]
        S2["Alt Musteriler<br/>Atanan Musteriler"]
        S3["Sadece Kendisi<br/>Kendi Verileri"]
    end

    ADMIN --> S1
    BROKER --> S2
    CUSTOMER --> S3

    ADMIN -.->|yonetir| BROKER
    BROKER -.->|yonetir| CUSTOMER
```

### Broker - Musteri Iliskisi (Many-to-Many)

```mermaid
flowchart LR
    subgraph Brokers["Broker'lar"]
        B1["Broker: Ahmet<br/>brokerId: 101"]
        B2["Broker: Mehmet<br/>brokerId: 102"]
        B3["Broker: Ayse<br/>brokerId: 103"]
    end

    subgraph Customers["Musteriler"]
        C1["Customer: Ali<br/>customerId: 201"]
        C2["Customer: Veli<br/>customerId: 202"]
        C3["Customer: Can<br/>customerId: 203"]
    end

    B1 --> C1
    B1 --> C2
    B2 --> C1
    B2 --> C3
    B3 --> C2
    B3 --> C3
```

### Ortak Musteri Senaryosu

```mermaid
flowchart TB
    subgraph Scenario["Senaryo: Ali - customerId 201"]
        C["Customer: Ali"]
    end

    subgraph AuthorizedBrokers["Yetkili Broker'lar"]
        B1["Ahmet - brokerId 101"]
        B2["Mehmet - brokerId 102"]
    end

    subgraph Actions["Her iki broker da yapabilir"]
        A1["Ali icin emir olustur"]
        A2["Ali'nin emirlerini gor"]
        A3["Ali'nin varliklarini gor"]
        A4["Ali'nin emrini iptal et"]
    end

    B1 --> C
    B2 --> C
    C --> Actions
```

### Veritabani Iliskisi

```mermaid
erDiagram
    BROKER ||--o{ BROKER_CUSTOMER : manages
    CUSTOMER ||--o{ BROKER_CUSTOMER : managed_by

    BROKER {
        long brokerId PK
        string name
        string email
    }

    CUSTOMER {
        long customerId PK
        string name
        string email
    }

    BROKER_CUSTOMER {
        long brokerId FK
        long customerId FK
        date assignedAt
        boolean active
    }
```

### Keycloak Rol Atamasi

```mermaid
flowchart TB
    subgraph Keycloak["Keycloak Realm: brokage"]
        subgraph RealmRoles["Realm Roles"]
            ADMIN_ROLE[ADMIN]
            BROKER_ROLE[BROKER]
            CUSTOMER_ROLE[CUSTOMER]
        end

        subgraph Users["Kullanicilar"]
            U1["admin@brokage.com"]
            U2["ahmet@brokage.com"]
            U3["mehmet@brokage.com"]
            U4["ali@customer.com"]
            U5["veli@customer.com"]
        end
    end

    U1 --> ADMIN_ROLE
    U2 --> BROKER_ROLE
    U3 --> BROKER_ROLE
    U4 --> CUSTOMER_ROLE
    U5 --> CUSTOMER_ROLE
```

### Yetkilendirme Karar Akisi

```mermaid
flowchart TB
    REQ[Gelen Istek] --> AUTH{Token Var mi?}

    AUTH -->|Hayir| U401[401 Unauthorized]
    AUTH -->|Evet| VALID{Token Gecerli mi?}

    VALID -->|Hayir| U401
    VALID -->|Evet| EXTRACT[Claims Cikart]

    EXTRACT --> ROLE{Rol Nedir?}

    ROLE -->|ADMIN| ADMIN_OK[Tam Yetki - Tum Musteriler]
    ROLE -->|BROKER| BROKER_CHECK{Alt Musterisi mi?}
    ROLE -->|CUSTOMER| CUST_CHECK{Kendi Verisi mi?}

    ADMIN_OK --> OK[200 OK]

    BROKER_CHECK -->|Evet| BROKER_OK[Yetkili - Alt Musteri]
    BROKER_CHECK -->|Hayir| U403[403 Forbidden]
    BROKER_OK --> OK

    CUST_CHECK -->|Evet| CUST_OK[Yetkili - Kendi Verisi]
    CUST_CHECK -->|Hayir| U403
    CUST_OK --> OK

    style U401 fill:#f66
    style U403 fill:#f96
    style OK fill:#6f6
```

### Endpoint Yetki Matrisi - 3 Seviye

```mermaid
flowchart TB
    subgraph Legend["Roller"]
        direction LR
        L1["ADMIN"]
        L2["BROKER"]
        L3["CUSTOMER"]
    end

    subgraph Matrix["Yetki Matrisi"]
        subgraph Orders["Emir Islemleri"]
            O1["POST /orders"]
            O2["GET /orders"]
            O3["DELETE /orders/id"]
            O4["POST /orders/id/match"]
        end

        subgraph Assets["Varlik Islemleri"]
            A1["GET /assets"]
            A2["POST /deposit"]
            A3["POST /withdraw"]
        end
    end

    subgraph AdminScope["ADMIN Yetkisi"]
        AD1[Tum musteriler icin emir]
        AD2[Tum emirleri gor]
        AD3[Tum emirleri iptal]
        AD4[Eslestirme yetkisi]
        AD5[Tum varliklari gor]
        AD6[Para yatir - tum musteriler]
        AD7[Para cek - tum musteriler]
    end

    subgraph BrokerScope["BROKER Yetkisi"]
        BR1[Alt musteriler icin emir]
        BR2[Alt musteri emirleri]
        BR3[Alt musteri emirlerini iptal]
        BR4[Alt musteri emirlerini eslestir]
        BR5[Alt musteri varliklari]
        BR6[Alt musteriye yatir]
        BR7[Erisim Yok]
    end

    subgraph CustomerScope["CUSTOMER Yetkisi"]
        CU1[Sadece kendisi icin]
        CU2[Sadece kendi emirleri]
        CU3[Kendi emri + PENDING]
        CU4[Erisim Yok]
        CU5[Sadece kendi varliklari]
        CU6[Erisim Yok]
        CU7[Kendi hesabindan cek]
    end

    style BR4 fill:#9f9
    style BR7 fill:#f99
    style CU4 fill:#f99
    style CU6 fill:#f99
    style AD4 fill:#9f9
```

### Broker Yetki Kontrolu Detay

```mermaid
sequenceDiagram
    participant B as Broker Ahmet
    participant T as Traefik
    participant S as Order Service
    participant DB as Database

    B->>T: GET /api/orders?customerId=201<br/>JWT: brokerId=101

    T->>S: Forward Request

    S->>DB: SELECT * FROM broker_customers<br/>WHERE brokerId=101
    DB-->>S: customerIds: 201, 202

    S->>S: 201 in managedCustomers?

    alt Evet - Alt Musterisi
        S->>DB: SELECT * FROM orders<br/>WHERE customerId=201
        DB-->>S: Orders
        S-->>B: 200 OK - Orders
    else Hayir - Baska Musteri
        S-->>B: 403 Forbidden<br/>Bu musteriye erisim yetkiniz yok
    end
```

---

## Yonlendirme Kurallari

```mermaid
flowchart TB
    subgraph Gateway["Traefik Gateway"]
        subgraph Rules["Yonlendirme Kurallari"]
            R1["api.brokage.local - orders"]
            R2["api.brokage.local - assets"]
            R3["api.brokage.local - customers"]
            R4["api.brokage.local - notifications"]
            R5["api.brokage.local - ws"]
            R6["auth.brokage.local"]
            R7["monitor.brokage.local"]
        end
    end

    subgraph Services["Hedef Servisler"]
        S1["order-service 8080"]
        S2["asset-service 8080"]
        S3["customer-service 8080"]
        S4["notification-service 8080"]
        S5["notification-service 8080 WS"]
        S6["keycloak 8080"]
        S7["grafana 3000"]
    end

    R1 --> S1
    R2 --> S2
    R3 --> S3
    R4 --> S4
    R5 --> S5
    R6 --> S6
    R7 --> S7
```

---

## Saglik Kontrolu

```mermaid
sequenceDiagram
    participant T as Traefik
    participant S1 as Order Service
    participant S2 as Asset Service
    participant LB as Load Balancer

    loop Her 10 saniye
        T->>S1: GET /actuator/health
        S1-->>T: 200 OK

        T->>S2: GET /actuator/health
        S2-->>T: 200 OK
    end

    Note over T,LB: Servis Sagliksiz Olursa

    T->>S1: GET /actuator/health
    S1-->>T: 503 Service Unavailable

    T->>LB: S1'i havuzdan cikar

    Note over S1: Duzeltme sonrasi

    T->>S1: GET /actuator/health
    S1-->>T: 200 OK

    T->>LB: S1'i havuza ekle
```

---

## Yuk Dengeleme

```mermaid
flowchart TB
    subgraph Traefik["Traefik Load Balancer"]
        LB[Round Robin]
    end

    subgraph OrderCluster["Order Service Cluster"]
        OS1[Order Service 1]
        OS2[Order Service 2]
        OS3[Order Service 3]
    end

    subgraph Health["Saglik Durumu"]
        H1[Saglikli]
        H2[Saglikli]
        H3[Sagliksiz]
    end

    LB --> OS1
    LB --> OS2
    LB -.-> OS3

    OS1 --> H1
    OS2 --> H2
    OS3 --> H3

    style OS3 fill:#f99
    style H3 fill:#f99
```

---

## HTTPS ve TLS

```mermaid
flowchart LR
    subgraph Client["Istemci"]
        BROWSER[Tarayici]
    end

    subgraph TLS["TLS Sonlandirma"]
        TRAEFIK["Traefik 443"]
        CERT["Lets Encrypt"]
    end

    subgraph Internal["Ic Ag"]
        SERVICES["Servisler 8080"]
    end

    BROWSER -->|HTTPS| TRAEFIK
    TRAEFIK -->|Sertifika| CERT
    TRAEFIK -->|HTTP| SERVICES

    Note1["Dis: Sifrelenmis<br/>Ic: Duz HTTP"]
```

---

## Guvenlik Katmanlari

```mermaid
flowchart TB
    subgraph Layer1["Ag Katmani"]
        FIREWALL[Firewall]
        WAF[Web Application Firewall]
        DDOS[DDoS Korumasi]
    end

    subgraph Layer2["Gateway Katmani"]
        RL[Rate Limiting]
        IP[IP Filtreleme]
        TLS[TLS/HTTPS]
    end

    subgraph Layer3["Uygulama Katmani"]
        AUTH[JWT Authentication]
        AUTHZ[Authorization]
        VALID[Input Validation]
    end

    subgraph Layer4["Veri Katmani"]
        ENC[Sifreleme]
        AUDIT[Audit Logging]
        MASK[Data Masking]
    end

    Layer1 --> Layer2
    Layer2 --> Layer3
    Layer3 --> Layer4
```

---

## Middleware Zinciri

Istek isleme sirasi:

```mermaid
flowchart LR
    REQ[Istek] --> RL
    RL[Rate Limit] --> SEC
    SEC[Security Headers] --> CORS
    CORS[CORS] --> JWT
    JWT[JWT Auth] --> CB
    CB[Circuit Breaker] --> SVC
    SVC[Servis] --> RES[Yanit]

    style RL fill:#ff9
    style SEC fill:#9f9
    style CORS fill:#99f
    style JWT fill:#f9f
    style CB fill:#f99
```

---

## Hata Yonetimi

```mermaid
flowchart TB
    subgraph Errors["Hata Turleri"]
        E401[401 Unauthorized]
        E403[403 Forbidden]
        E429[429 Too Many Requests]
        E500[500 Internal Error]
        E503[503 Service Unavailable]
    end

    subgraph Causes["Nedenler"]
        C1[Gecersiz Token]
        C2[Yetersiz Yetki]
        C3[Rate Limit Asildi]
        C4[Sunucu Hatasi]
        C5[Circuit Open]
    end

    subgraph Actions["Aksiyonlar"]
        A1[Yeniden Login]
        A2[Yetki Kontrolu]
        A3[Bekleme]
        A4[Retry]
        A5[Fallback]
    end

    E401 --> C1
    E403 --> C2
    E429 --> C3
    E500 --> C4
    E503 --> C5

    C1 --> A1
    C2 --> A2
    C3 --> A3
    C4 --> A4
    C5 --> A5
```

---

## Sonraki Adimlar

- **[Monitoring ve Observability](06-monitoring-observability.md)** - LGTM Stack
