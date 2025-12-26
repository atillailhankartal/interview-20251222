# Brokage API - Postman Collection

Brokage Trading System API'leri icin Postman collection ve environment dosyalari.

## Kurulum

1. Postman'i acin
2. **Import** butonuna tiklayin
3. Her iki dosyayi da import edin:
   - `Brokage-API.postman_collection.json`
   - `Brokage-Environment.postman_environment.json`
4. Sag ust koseden **Brokage Local Environment** secin

## Kullanici Bilgileri

| Rol | Username | Password |
|-----|----------|----------|
| Admin | nick.fury | admin123 |
| Broker | tony.stark | broker123 |
| Customer | peter.parker | customer123 |

## Hizli Baslangic

### 1. Token Alma

**Authentication** klasorunden ilgili token request'ini calistirin:
- `Get Admin Token` - Admin olarak giris
- `Get Broker Token` - Broker olarak giris
- `Get Customer Token` - Customer olarak giris

Token otomatik olarak environment variable'a kaydedilir.

### 2. Islemler

Token aldiktan sonra ilgili klasorlerden request'leri calistirabilirsiniz:
- **Customer Service** - Musteri yonetimi
- **Asset Service** - Varlik/bakiye islemleri
- **Order Service** - Emir islemleri
- **Admin Operations** - Admin islemleri

### 3. Test Senaryolari

`Test Scenarios > 1. Full Trading Flow` klasorunu **Collection Runner** ile calistirarak tam bir trading akisi test edebilirsiniz.

## Environment Variables

| Variable | Aciklama |
|----------|----------|
| keycloak_url | Keycloak URL (default: http://localhost:8180) |
| order_service_url | Order Service URL (default: http://localhost:7081) |
| asset_service_url | Asset Service URL (default: http://localhost:7082) |
| customer_service_url | Customer Service URL (default: http://localhost:7083) |
| admin_token | Admin JWT token (otomatik set edilir) |
| broker_token | Broker JWT token (otomatik set edilir) |
| customer_token | Customer JWT token (otomatik set edilir) |
| test_customer_id | Test sirasinda kullanilan customer ID |
| test_order_id | Test sirasinda kullanilan order ID |

## Notlar

- Servisler `docker-compose up -d` ile calisir durumda olmalidir
- Token'lar 1 saat gecerlidir, suresi dolunca yeniden token alin
- Her token istegi sonrasi `current_token` degiskeni de guncellenir
