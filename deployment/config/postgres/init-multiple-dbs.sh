#!/bin/bash
# =============================================================================
# PostgreSQL - Multiple Database Initialization + Seed Data
# =============================================================================

set -e
set -u

function create_database() {
    local database=$1
    echo "Creating database: $database"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
}

# Create Keycloak database
create_database "keycloak"

# Create application databases
if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Multiple databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_database $db
    done
    echo "Multiple databases created successfully!"
fi

# =============================================================================
# Seed Data for Demo
# =============================================================================

echo "Seeding demo data..."

# Seed Customers Database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "brokage_customers" <<-EOSQL
    -- Customers table
    CREATE TABLE IF NOT EXISTS customers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        keycloak_id VARCHAR(255) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        first_name VARCHAR(100) NOT NULL,
        last_name VARCHAR(100) NOT NULL,
        tier VARCHAR(20) DEFAULT 'STANDARD',
        status VARCHAR(20) DEFAULT 'ACTIVE',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Seed customers (matching Keycloak users)
    INSERT INTO customers (keycloak_id, email, first_name, last_name, tier, status) VALUES
        ('admin-user-id', 'admin@brokage.com', 'Admin', 'User', 'VIP', 'ACTIVE'),
        ('broker1-user-id', 'broker1@brokage.com', 'Broker', 'One', 'PREMIUM', 'ACTIVE'),
        ('broker2-user-id', 'broker2@brokage.com', 'Broker', 'Two', 'PREMIUM', 'ACTIVE'),
        ('customer1-user-id', 'customer1@brokage.com', 'Customer', 'One', 'STANDARD', 'ACTIVE'),
        ('customer2-user-id', 'customer2@brokage.com', 'Customer', 'Two', 'STANDARD', 'ACTIVE')
    ON CONFLICT (keycloak_id) DO NOTHING;

    -- Broker-Customer relationships
    CREATE TABLE IF NOT EXISTS broker_customers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        broker_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(broker_id, customer_id)
    );
EOSQL

# Seed Assets Database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "brokage_assets" <<-EOSQL
    -- Assets table (stock symbols)
    CREATE TABLE IF NOT EXISTS assets (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        symbol VARCHAR(10) UNIQUE NOT NULL,
        name VARCHAR(255) NOT NULL,
        asset_type VARCHAR(50) DEFAULT 'STOCK',
        status VARCHAR(20) DEFAULT 'ACTIVE',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Customer assets (portfolio)
    CREATE TABLE IF NOT EXISTS customer_assets (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        customer_id UUID NOT NULL,
        asset_symbol VARCHAR(10) NOT NULL,
        quantity DECIMAL(18,8) NOT NULL DEFAULT 0,
        usable_quantity DECIMAL(18,8) NOT NULL DEFAULT 0,
        average_cost DECIMAL(18,8) DEFAULT 0,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(customer_id, asset_symbol)
    );

    -- Seed available assets (Turkish stocks + TRY)
    INSERT INTO assets (symbol, name, asset_type, status) VALUES
        ('TRY', 'Turkish Lira', 'CURRENCY', 'ACTIVE'),
        ('THYAO', 'Türk Hava Yolları', 'STOCK', 'ACTIVE'),
        ('GARAN', 'Garanti Bankası', 'STOCK', 'ACTIVE'),
        ('AKBNK', 'Akbank', 'STOCK', 'ACTIVE'),
        ('EREGL', 'Ereğli Demir Çelik', 'STOCK', 'ACTIVE'),
        ('BIMAS', 'BİM Mağazalar', 'STOCK', 'ACTIVE'),
        ('KCHOL', 'Koç Holding', 'STOCK', 'ACTIVE'),
        ('SAHOL', 'Sabancı Holding', 'STOCK', 'ACTIVE'),
        ('SISE', 'Şişecam', 'STOCK', 'ACTIVE'),
        ('TUPRS', 'Tüpraş', 'STOCK', 'ACTIVE'),
        ('TCELL', 'Turkcell', 'STOCK', 'ACTIVE')
    ON CONFLICT (symbol) DO NOTHING;

    -- Seed customer portfolios with initial TRY balance
    INSERT INTO customer_assets (customer_id, asset_symbol, quantity, usable_quantity)
    SELECT c.id, 'TRY', 100000.00, 100000.00
    FROM (SELECT gen_random_uuid() as id) c
    WHERE NOT EXISTS (SELECT 1 FROM customer_assets LIMIT 1);
EOSQL

# Seed Orders Database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "brokage_orders" <<-EOSQL
    -- Orders table
    CREATE TABLE IF NOT EXISTS orders (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        customer_id UUID NOT NULL,
        asset_symbol VARCHAR(10) NOT NULL,
        order_side VARCHAR(10) NOT NULL,
        order_type VARCHAR(20) DEFAULT 'LIMIT',
        quantity DECIMAL(18,8) NOT NULL,
        price DECIMAL(18,8) NOT NULL,
        filled_quantity DECIMAL(18,8) DEFAULT 0,
        status VARCHAR(20) DEFAULT 'PENDING',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Outbox table for transactional messaging
    CREATE TABLE IF NOT EXISTS outbox_events (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        aggregate_type VARCHAR(100) NOT NULL,
        aggregate_id VARCHAR(255) NOT NULL,
        event_type VARCHAR(100) NOT NULL,
        payload JSONB NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        processed_at TIMESTAMP,
        status VARCHAR(20) DEFAULT 'PENDING'
    );

    -- Processed events for idempotency
    CREATE TABLE IF NOT EXISTS processed_events (
        event_id VARCHAR(255) PRIMARY KEY,
        processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
    CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
    CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status);
EOSQL

# Seed Order Processor Database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "brokage_processor" <<-EOSQL
    -- Order matching queue
    CREATE TABLE IF NOT EXISTS order_queue (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        order_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        asset_symbol VARCHAR(10) NOT NULL,
        order_side VARCHAR(10) NOT NULL,
        quantity DECIMAL(18,8) NOT NULL,
        price DECIMAL(18,8) NOT NULL,
        priority INT DEFAULT 0,
        status VARCHAR(20) DEFAULT 'QUEUED',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Trade executions
    CREATE TABLE IF NOT EXISTS trades (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        buy_order_id UUID NOT NULL,
        sell_order_id UUID NOT NULL,
        asset_symbol VARCHAR(10) NOT NULL,
        quantity DECIMAL(18,8) NOT NULL,
        price DECIMAL(18,8) NOT NULL,
        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_queue_symbol_side ON order_queue(asset_symbol, order_side, status);
    CREATE INDEX IF NOT EXISTS idx_queue_priority ON order_queue(priority DESC, created_at ASC);
EOSQL

echo "Database seed completed successfully!"
