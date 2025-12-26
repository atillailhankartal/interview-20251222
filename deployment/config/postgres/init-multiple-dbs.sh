#!/bin/bash
# =============================================================================
# PostgreSQL - Single Database Initialization + Comprehensive Demo Seed Data
# =============================================================================
# Characters from: Marvel, Lord of the Rings, Hobbit, Matrix, Harry Potter, Justice League
#
# Summary:
#   - 1 Admin: Nick Fury (The Director)
#   - 10 Brokers: Leaders from each universe
#   - 100 Customers: Heroes, villains, and sidekicks
#   - Various orders (PENDING, MATCHED, CANCELED)
#   - Asset portfolios with TRY and stocks
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

echo "Initializing brokage database schema and comprehensive seed data..."

# =============================================================================
# Create all tables in single brokage database
# =============================================================================

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "brokage" <<-'EOSQL'
    -- =========================================================================
    -- CUSTOMERS (Customer Service)
    -- =========================================================================
    CREATE TABLE IF NOT EXISTS customers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        first_name VARCHAR(100) NOT NULL,
        last_name VARCHAR(100) NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        phone_number VARCHAR(20),
        identity_number VARCHAR(11) UNIQUE NOT NULL,
        birth_date DATE,
        tier VARCHAR(20) DEFAULT 'STANDARD',
        status VARCHAR(20) DEFAULT 'ACTIVE',
        role VARCHAR(20) DEFAULT 'CUSTOMER',
        keycloak_user_id VARCHAR(36),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_customer_email ON customers(email);
    CREATE INDEX IF NOT EXISTS idx_customer_identity_number ON customers(identity_number);
    CREATE INDEX IF NOT EXISTS idx_customer_tier ON customers(tier);
    CREATE INDEX IF NOT EXISTS idx_customer_status ON customers(status);
    CREATE INDEX IF NOT EXISTS idx_customer_role ON customers(role);

    -- Broker-Customer relationships
    CREATE TABLE IF NOT EXISTS broker_customers (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        broker_id UUID NOT NULL,
        customer_id UUID NOT NULL,
        active BOOLEAN DEFAULT true,
        notes VARCHAR(500),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0,
        UNIQUE(broker_id, customer_id)
    );

    CREATE INDEX IF NOT EXISTS idx_broker_customers_broker ON broker_customers(broker_id);
    CREATE INDEX IF NOT EXISTS idx_broker_customers_customer ON broker_customers(customer_id);

    -- =========================================================================
    -- ASSETS (Asset Service)
    -- =========================================================================

    -- Assets table (stock symbols)
    CREATE TABLE IF NOT EXISTS assets (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        symbol VARCHAR(10) UNIQUE NOT NULL,
        name VARCHAR(255) NOT NULL,
        asset_type VARCHAR(50) DEFAULT 'STOCK',
        status VARCHAR(20) DEFAULT 'ACTIVE',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Customer assets (portfolio) - PDF compliant schema
    CREATE TABLE IF NOT EXISTS customer_assets (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        customer_id UUID NOT NULL,
        asset_name VARCHAR(20) NOT NULL,
        size DECIMAL(19,4) NOT NULL DEFAULT 0,
        usable_size DECIMAL(19,4) NOT NULL DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0,
        UNIQUE(customer_id, asset_name)
    );

    CREATE INDEX IF NOT EXISTS idx_customer_assets_customer ON customer_assets(customer_id);
    CREATE INDEX IF NOT EXISTS idx_customer_assets_name ON customer_assets(asset_name);

    -- =========================================================================
    -- ORDERS (Order Service) - PDF compliant schema
    -- =========================================================================
    CREATE TABLE IF NOT EXISTS orders (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        customer_id UUID NOT NULL,
        asset_name VARCHAR(20) NOT NULL,
        order_side VARCHAR(10) NOT NULL,
        order_type VARCHAR(20) DEFAULT 'LIMIT',
        size DECIMAL(19,4) NOT NULL,
        price DECIMAL(19,4) NOT NULL,
        filled_size DECIMAL(19,4) DEFAULT 0,
        status VARCHAR(20) DEFAULT 'PENDING',
        customer_tier_priority INT,
        idempotency_key VARCHAR(64) UNIQUE,
        rejection_reason VARCHAR(500),
        matched_at TIMESTAMP,
        cancelled_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
    CREATE INDEX IF NOT EXISTS idx_orders_asset ON orders(asset_name);
    CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
    CREATE INDEX IF NOT EXISTS idx_orders_side ON orders(order_side);
    CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at);

    -- =========================================================================
    -- ORDER PROCESSOR
    -- =========================================================================

    -- Matching queue
    CREATE TABLE IF NOT EXISTS matching_queue (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        order_id UUID NOT NULL UNIQUE,
        customer_id UUID NOT NULL,
        asset_name VARCHAR(20) NOT NULL,
        order_side VARCHAR(10) NOT NULL,
        price DECIMAL(19,4) NOT NULL,
        remaining_size DECIMAL(19,4) NOT NULL,
        tier_priority INT NOT NULL DEFAULT 0,
        status VARCHAR(20) DEFAULT 'ACTIVE',
        queued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        matched_at TIMESTAMP,
        removed_at TIMESTAMP,
        remove_reason VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_queue_asset_side ON matching_queue(asset_name, order_side);
    CREATE INDEX IF NOT EXISTS idx_queue_priority ON matching_queue(tier_priority, price, queued_at);
    CREATE INDEX IF NOT EXISTS idx_queue_order ON matching_queue(order_id);
    CREATE INDEX IF NOT EXISTS idx_queue_status ON matching_queue(status);

    -- Trade executions
    CREATE TABLE IF NOT EXISTS trades (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        buy_order_id UUID NOT NULL,
        sell_order_id UUID NOT NULL,
        buyer_customer_id UUID NOT NULL,
        seller_customer_id UUID NOT NULL,
        asset_name VARCHAR(20) NOT NULL,
        quantity DECIMAL(19,4) NOT NULL,
        price DECIMAL(19,4) NOT NULL,
        total_value DECIMAL(19,4) NOT NULL,
        taker_side VARCHAR(10) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_trade_buy_order ON trades(buy_order_id);
    CREATE INDEX IF NOT EXISTS idx_trade_sell_order ON trades(sell_order_id);
    CREATE INDEX IF NOT EXISTS idx_trade_asset ON trades(asset_name);
    CREATE INDEX IF NOT EXISTS idx_trade_created ON trades(created_at);

    -- Saga instances
    CREATE TABLE IF NOT EXISTS saga_instances (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        correlation_id UUID NOT NULL UNIQUE,
        saga_type VARCHAR(100) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'STARTED',
        current_step VARCHAR(100) NOT NULL,
        payload TEXT,
        context TEXT,
        completed_steps TEXT,
        failed_step VARCHAR(100),
        error_message VARCHAR(2000),
        retry_count INT DEFAULT 0,
        max_retries INT DEFAULT 3,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP,
        expires_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        version BIGINT DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_saga_correlation ON saga_instances(correlation_id);
    CREATE INDEX IF NOT EXISTS idx_saga_type ON saga_instances(saga_type);
    CREATE INDEX IF NOT EXISTS idx_saga_status ON saga_instances(status);
    CREATE INDEX IF NOT EXISTS idx_saga_created ON saga_instances(created_at);

    -- =========================================================================
    -- OUTBOX PATTERN (Single table for all services)
    -- Matches OutboxEvent.java entity exactly
    -- =========================================================================
    CREATE TABLE IF NOT EXISTS outbox_events (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        aggregate_id UUID NOT NULL,
        aggregate_type VARCHAR(100) NOT NULL,
        event_type VARCHAR(100) NOT NULL,
        topic VARCHAR(255) NOT NULL,
        partition_key VARCHAR(255),
        payload TEXT NOT NULL,
        processed BOOLEAN NOT NULL DEFAULT FALSE,
        processed_at TIMESTAMP,
        retry_count INTEGER DEFAULT 0,
        error_message VARCHAR(1000),
        trace_id VARCHAR(64),
        span_id VARCHAR(32),
        ip_address VARCHAR(45),
        user_agent VARCHAR(500),
        request_id VARCHAR(64),
        performed_by UUID,
        performed_by_role VARCHAR(50),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_outbox_processed ON outbox_events(processed);
    CREATE INDEX IF NOT EXISTS idx_outbox_created ON outbox_events(created_at);
    CREATE INDEX IF NOT EXISTS idx_outbox_type ON outbox_events(event_type);

    -- Processed events for idempotency
    CREATE TABLE IF NOT EXISTS processed_events (
        event_id VARCHAR(255) PRIMARY KEY,
        processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- =========================================================================
    -- SEED DATA - CHARACTERS FROM MULTIPLE UNIVERSES
    -- =========================================================================

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
        ('TCELL', 'Turkcell', 'STOCK', 'ACTIVE'),
        ('ASELS', 'Aselsan', 'STOCK', 'ACTIVE'),
        ('PGSUS', 'Pegasus', 'STOCK', 'ACTIVE'),
        ('TAVHL', 'TAV Havalimanları', 'STOCK', 'ACTIVE'),
        ('VESTL', 'Vestel', 'STOCK', 'ACTIVE')
    ON CONFLICT (symbol) DO NOTHING;

    -- =========================================================================
    -- ADMIN (1) - Nick Fury - The Director
    -- Fixed UUID for testing stability
    -- =========================================================================
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('a0000001-0001-0001-0001-000000000001', 'Nick', 'Fury', 'nick.fury@brokage.com', '+905551000001', '10000000001', 'VIP', 'ACTIVE', 'ADMIN')
    ON CONFLICT (email) DO NOTHING;

    -- =========================================================================
    -- BROKERS (10) - Leaders from each universe
    -- Fixed UUIDs for testing stability
    -- =========================================================================
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        -- Marvel Leaders
        ('b0000001-0001-0001-0001-000000000001', 'Tony', 'Stark', 'tony.stark@brokage.com', '+905552000001', '20000000001', 'VIP', 'ACTIVE', 'BROKER'),
        ('b0000001-0001-0001-0001-000000000002', 'Steve', 'Rogers', 'steve.rogers@brokage.com', '+905552000002', '20000000002', 'VIP', 'ACTIVE', 'BROKER'),
        -- LOTR Leaders
        ('b0000001-0001-0001-0001-000000000003', 'Gandalf', 'Grey', 'gandalf.grey@brokage.com', '+905552000003', '20000000003', 'VIP', 'ACTIVE', 'BROKER'),
        ('b0000001-0001-0001-0001-000000000004', 'Aragorn', 'Elessar', 'aragorn.elessar@brokage.com', '+905552000004', '20000000004', 'VIP', 'ACTIVE', 'BROKER'),
        -- Matrix Leader
        ('b0000001-0001-0001-0001-000000000005', 'Morpheus', 'Ship', 'morpheus.ship@brokage.com', '+905552000005', '20000000005', 'VIP', 'ACTIVE', 'BROKER'),
        -- Harry Potter Leaders
        ('b0000001-0001-0001-0001-000000000006', 'Albus', 'Dumbledore', 'albus.dumbledore@brokage.com', '+905552000006', '20000000006', 'VIP', 'ACTIVE', 'BROKER'),
        ('b0000001-0001-0001-0001-000000000007', 'Severus', 'Snape', 'severus.snape@brokage.com', '+905552000007', '20000000007', 'VIP', 'ACTIVE', 'BROKER'),
        -- Justice League Leaders
        ('b0000001-0001-0001-0001-000000000008', 'Bruce', 'Wayne', 'bruce.wayne@brokage.com', '+905552000008', '20000000008', 'VIP', 'ACTIVE', 'BROKER'),
        ('b0000001-0001-0001-0001-000000000009', 'Diana', 'Prince', 'diana.prince@brokage.com', '+905552000009', '20000000009', 'VIP', 'ACTIVE', 'BROKER'),
        ('b0000001-0001-0001-0001-000000000010', 'Clark', 'Kent', 'clark.kent@brokage.com', '+905552000010', '20000000010', 'VIP', 'ACTIVE', 'BROKER')
    ON CONFLICT (email) DO NOTHING;

    -- =========================================================================
    -- CUSTOMERS (100) - Heroes, Villains, and Sidekicks
    -- Fixed UUIDs for testing stability (c = customer prefix)
    -- =========================================================================

    -- MARVEL UNIVERSE (25 customers)
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('c0000001-0001-0001-0001-000000000001', 'Peter', 'Parker', 'peter.parker@brokage.com', '+905553000001', '30000000001', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000002', 'Bruce', 'Banner', 'bruce.banner@brokage.com', '+905553000002', '30000000002', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000003', 'Thor', 'Odinson', 'thor.odinson@brokage.com', '+905553000003', '30000000003', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000004', 'Natasha', 'Romanoff', 'natasha.romanoff@brokage.com', '+905553000004', '30000000004', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000005', 'Clint', 'Barton', 'clint.barton@brokage.com', '+905553000005', '30000000005', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000006', 'Wanda', 'Maximoff', 'wanda.maximoff@brokage.com', '+905553000006', '30000000006', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000007', 'Vision', 'Synthezoid', 'vision.synthezoid@brokage.com', '+905553000007', '30000000007', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000008', 'Scott', 'Lang', 'scott.lang@brokage.com', '+905553000008', '30000000008', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000009', 'Hope', 'VanDyne', 'hope.vandyne@brokage.com', '+905553000009', '30000000009', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000010', 'TChalla', 'Wakanda', 'tchalla.wakanda@brokage.com', '+905553000010', '30000000010', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000011', 'Shuri', 'Wakanda', 'shuri.wakanda@brokage.com', '+905553000011', '30000000011', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000012', 'Stephen', 'Strange', 'stephen.strange@brokage.com', '+905553000012', '30000000012', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000013', 'Carol', 'Danvers', 'carol.danvers@brokage.com', '+905553000013', '30000000013', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000014', 'Bucky', 'Barnes', 'bucky.barnes@brokage.com', '+905553000014', '30000000014', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000015', 'Sam', 'Wilson', 'sam.wilson@brokage.com', '+905553000015', '30000000015', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000016', 'Peter', 'Quill', 'peter.quill@brokage.com', '+905553000016', '30000000016', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000017', 'Gamora', 'Zen', 'gamora.zen@brokage.com', '+905553000017', '30000000017', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000018', 'Rocket', 'Raccoon', 'rocket.raccoon@brokage.com', '+905553000018', '30000000018', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000019', 'Groot', 'Flora', 'groot.flora@brokage.com', '+905553000019', '30000000019', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000020', 'Drax', 'Destroyer', 'drax.destroyer@brokage.com', '+905553000020', '30000000020', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000021', 'Pepper', 'Potts', 'pepper.potts@brokage.com', '+905553000021', '30000000021', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000022', 'Happy', 'Hogan', 'happy.hogan@brokage.com', '+905553000022', '30000000022', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000023', 'Loki', 'Laufeyson', 'loki.laufeyson@brokage.com', '+905553000023', '30000000023', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000024', 'Thanos', 'Titan', 'thanos.titan@brokage.com', '+905553000024', '30000000024', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000025', 'Nebula', 'Titan', 'nebula.titan@brokage.com', '+905553000025', '30000000025', 'STANDARD', 'ACTIVE', 'CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

    -- LORD OF THE RINGS / HOBBIT (20 customers)
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('c0000001-0001-0001-0001-000000000026', 'Frodo', 'Baggins', 'frodo.baggins@brokage.com', '+905553000026', '30000000026', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000027', 'Samwise', 'Gamgee', 'samwise.gamgee@brokage.com', '+905553000027', '30000000027', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000028', 'Legolas', 'Greenleaf', 'legolas.greenleaf@brokage.com', '+905553000028', '30000000028', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000029', 'Gimli', 'Gloin', 'gimli.gloin@brokage.com', '+905553000029', '30000000029', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000030', 'Boromir', 'Gondor', 'boromir.gondor@brokage.com', '+905553000030', '30000000030', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000031', 'Faramir', 'Gondor', 'faramir.gondor@brokage.com', '+905553000031', '30000000031', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000032', 'Eowyn', 'Rohan', 'eowyn.rohan@brokage.com', '+905553000032', '30000000032', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000033', 'Eomer', 'Rohan', 'eomer.rohan@brokage.com', '+905553000033', '30000000033', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000034', 'Theoden', 'King', 'theoden.king@brokage.com', '+905553000034', '30000000034', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000035', 'Bilbo', 'Baggins', 'bilbo.baggins@brokage.com', '+905553000035', '30000000035', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000036', 'Thorin', 'Oakenshield', 'thorin.oakenshield@brokage.com', '+905553000036', '30000000036', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000037', 'Galadriel', 'Lorien', 'galadriel.lorien@brokage.com', '+905553000037', '30000000037', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000038', 'Elrond', 'Rivendell', 'elrond.rivendell@brokage.com', '+905553000038', '30000000038', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000039', 'Arwen', 'Evenstar', 'arwen.evenstar@brokage.com', '+905553000039', '30000000039', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000040', 'Saruman', 'White', 'saruman.white@brokage.com', '+905553000040', '30000000040', 'VIP', 'INACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000041', 'Gollum', 'Smeagol', 'gollum.smeagol@brokage.com', '+905553000041', '30000000041', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000042', 'Treebeard', 'Ent', 'treebeard.ent@brokage.com', '+905553000042', '30000000042', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000043', 'Radagast', 'Brown', 'radagast.brown@brokage.com', '+905553000043', '30000000043', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000044', 'Merry', 'Brandybuck', 'merry.brandybuck@brokage.com', '+905553000044', '30000000044', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000045', 'Pippin', 'Took', 'pippin.took@brokage.com', '+905553000045', '30000000045', 'STANDARD', 'ACTIVE', 'CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

    -- MATRIX (10 customers)
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('c0000001-0001-0001-0001-000000000046', 'Neo', 'Anderson', 'neo.anderson@brokage.com', '+905553000046', '30000000046', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000047', 'Trinity', 'Matrix', 'trinity.matrix@brokage.com', '+905553000047', '30000000047', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000048', 'Agent', 'Smith', 'agent.smith@brokage.com', '+905553000048', '30000000048', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000049', 'Oracle', 'Cookie', 'oracle.cookie@brokage.com', '+905553000049', '30000000049', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000050', 'Niobe', 'Zion', 'niobe.zion@brokage.com', '+905553000050', '30000000050', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000051', 'Cypher', 'Reagan', 'cypher.reagan@brokage.com', '+905553000051', '30000000051', 'STANDARD', 'INACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000052', 'Tank', 'Operator', 'tank.operator@brokage.com', '+905553000052', '30000000052', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000053', 'Mouse', 'Programmer', 'mouse.programmer@brokage.com', '+905553000053', '30000000053', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000054', 'Merovingian', 'French', 'merovingian.french@brokage.com', '+905553000054', '30000000054', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000055', 'Seraph', 'Guardian', 'seraph.guardian@brokage.com', '+905553000055', '30000000055', 'PREMIUM', 'ACTIVE', 'CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

    -- HARRY POTTER (25 customers)
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('c0000001-0001-0001-0001-000000000056', 'Harry', 'Potter', 'harry.potter@brokage.com', '+905553000056', '30000000056', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000057', 'Hermione', 'Granger', 'hermione.granger@brokage.com', '+905553000057', '30000000057', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000058', 'Ron', 'Weasley', 'ron.weasley@brokage.com', '+905553000058', '30000000058', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000059', 'Ginny', 'Weasley', 'ginny.weasley@brokage.com', '+905553000059', '30000000059', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000060', 'Fred', 'Weasley', 'fred.weasley@brokage.com', '+905553000060', '30000000060', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000061', 'George', 'Weasley', 'george.weasley@brokage.com', '+905553000061', '30000000061', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000062', 'Neville', 'Longbottom', 'neville.longbottom@brokage.com', '+905553000062', '30000000062', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000063', 'Luna', 'Lovegood', 'luna.lovegood@brokage.com', '+905553000063', '30000000063', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000064', 'Draco', 'Malfoy', 'draco.malfoy@brokage.com', '+905553000064', '30000000064', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000065', 'Lucius', 'Malfoy', 'lucius.malfoy@brokage.com', '+905553000065', '30000000065', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000066', 'Minerva', 'McGonagall', 'minerva.mcgonagall@brokage.com', '+905553000066', '30000000066', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000067', 'Rubeus', 'Hagrid', 'rubeus.hagrid@brokage.com', '+905553000067', '30000000067', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000068', 'Sirius', 'Black', 'sirius.black@brokage.com', '+905553000068', '30000000068', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000069', 'Remus', 'Lupin', 'remus.lupin@brokage.com', '+905553000069', '30000000069', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000070', 'Nymphadora', 'Tonks', 'nymphadora.tonks@brokage.com', '+905553000070', '30000000070', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000071', 'Bellatrix', 'Lestrange', 'bellatrix.lestrange@brokage.com', '+905553000071', '30000000071', 'PREMIUM', 'INACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000072', 'Voldemort', 'Riddle', 'voldemort.riddle@brokage.com', '+905553000072', '30000000072', 'VIP', 'INACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000073', 'Dobby', 'House', 'dobby.house@brokage.com', '+905553000073', '30000000073', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000074', 'Cedric', 'Diggory', 'cedric.diggory@brokage.com', '+905553000074', '30000000074', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000075', 'Cho', 'Chang', 'cho.chang@brokage.com', '+905553000075', '30000000075', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000076', 'Viktor', 'Krum', 'viktor.krum@brokage.com', '+905553000076', '30000000076', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000077', 'Fleur', 'Delacour', 'fleur.delacour@brokage.com', '+905553000077', '30000000077', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000078', 'Molly', 'Weasley', 'molly.weasley@brokage.com', '+905553000078', '30000000078', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000079', 'Arthur', 'Weasley', 'arthur.weasley@brokage.com', '+905553000079', '30000000079', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000080', 'Horace', 'Slughorn', 'horace.slughorn@brokage.com', '+905553000080', '30000000080', 'PREMIUM', 'ACTIVE', 'CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

    -- JUSTICE LEAGUE / DC (20 customers)
    INSERT INTO customers (id, first_name, last_name, email, phone_number, identity_number, tier, status, role) VALUES
        ('c0000001-0001-0001-0001-000000000081', 'Barry', 'Allen', 'barry.allen@brokage.com', '+905553000081', '30000000081', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000082', 'Arthur', 'Curry', 'arthur.curry@brokage.com', '+905553000082', '30000000082', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000083', 'Victor', 'Stone', 'victor.stone@brokage.com', '+905553000083', '30000000083', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000084', 'Hal', 'Jordan', 'hal.jordan@brokage.com', '+905553000084', '30000000084', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000085', 'Oliver', 'Queen', 'oliver.queen@brokage.com', '+905553000085', '30000000085', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000086', 'Dinah', 'Lance', 'dinah.lance@brokage.com', '+905553000086', '30000000086', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000087', 'John', 'Constantine', 'john.constantine@brokage.com', '+905553000087', '30000000087', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000088', 'Zatanna', 'Zatara', 'zatanna.zatara@brokage.com', '+905553000088', '30000000088', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000089', 'Jonn', 'Jonzz', 'jonn.jonzz@brokage.com', '+905553000089', '30000000089', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000090', 'Shayera', 'Hol', 'shayera.hol@brokage.com', '+905553000090', '30000000090', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000091', 'Alfred', 'Pennyworth', 'alfred.pennyworth@brokage.com', '+905553000091', '30000000091', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000092', 'Lois', 'Lane', 'lois.lane@brokage.com', '+905553000092', '30000000092', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000093', 'Jimmy', 'Olsen', 'jimmy.olsen@brokage.com', '+905553000093', '30000000093', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000094', 'Lex', 'Luthor', 'lex.luthor@brokage.com', '+905553000094', '30000000094', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000095', 'Joker', 'Clown', 'joker.clown@brokage.com', '+905553000095', '30000000095', 'STANDARD', 'INACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000096', 'Harley', 'Quinn', 'harley.quinn@brokage.com', '+905553000096', '30000000096', 'STANDARD', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000097', 'Selina', 'Kyle', 'selina.kyle@brokage.com', '+905553000097', '30000000097', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000098', 'Oswald', 'Cobblepot', 'oswald.cobblepot@brokage.com', '+905553000098', '30000000098', 'VIP', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000099', 'Edward', 'Nigma', 'edward.nigma@brokage.com', '+905553000099', '30000000099', 'PREMIUM', 'ACTIVE', 'CUSTOMER'),
        ('c0000001-0001-0001-0001-000000000100', 'Pamela', 'Isley', 'pamela.isley@brokage.com', '+905553000100', '30000000100', 'STANDARD', 'ACTIVE', 'CUSTOMER')
    ON CONFLICT (email) DO NOTHING;

    -- =========================================================================
    -- BROKER-CUSTOMER ASSIGNMENTS
    -- =========================================================================
    -- Tony Stark (Marvel broker) -> Marvel customers (1-12)
    -- Steve Rogers (SHIELD broker) -> Marvel customers (13-25)
    -- Gandalf Grey (LOTR broker) -> LOTR customers (1-10)
    -- Aragorn Elessar (Men broker) -> LOTR customers (11-20)
    -- Morpheus Ship (Matrix broker) -> Matrix customers (1-10)
    -- Albus Dumbledore (HP broker) -> HP Gryffindor customers (1-12)
    -- Severus Snape (HP broker) -> HP Slytherin/others (13-25)
    -- Bruce Wayne (Batman broker) -> DC hero customers (1-10)
    -- Diana Prince (Wonder Woman broker) -> DC customers (11-17)
    -- Clark Kent (Superman broker) -> DC villain customers (18-20)

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Auto-assigned based on universe'
    FROM customers b, customers c
    WHERE b.email = 'tony.stark@brokage.com'
      AND c.email IN ('peter.parker@brokage.com', 'bruce.banner@brokage.com', 'thor.odinson@brokage.com',
                      'natasha.romanoff@brokage.com', 'clint.barton@brokage.com', 'wanda.maximoff@brokage.com',
                      'vision.synthezoid@brokage.com', 'scott.lang@brokage.com', 'hope.vandyne@brokage.com',
                      'tchalla.wakanda@brokage.com', 'shuri.wakanda@brokage.com', 'stephen.strange@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'SHIELD division assignment'
    FROM customers b, customers c
    WHERE b.email = 'steve.rogers@brokage.com'
      AND c.email IN ('carol.danvers@brokage.com', 'bucky.barnes@brokage.com', 'sam.wilson@brokage.com',
                      'peter.quill@brokage.com', 'gamora.zen@brokage.com', 'rocket.raccoon@brokage.com',
                      'groot.flora@brokage.com', 'drax.destroyer@brokage.com', 'pepper.potts@brokage.com',
                      'happy.hogan@brokage.com', 'loki.laufeyson@brokage.com', 'thanos.titan@brokage.com', 'nebula.titan@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Fellowship members'
    FROM customers b, customers c
    WHERE b.email = 'gandalf.grey@brokage.com'
      AND c.email IN ('frodo.baggins@brokage.com', 'samwise.gamgee@brokage.com', 'legolas.greenleaf@brokage.com',
                      'gimli.gloin@brokage.com', 'boromir.gondor@brokage.com', 'merry.brandybuck@brokage.com',
                      'pippin.took@brokage.com', 'bilbo.baggins@brokage.com', 'radagast.brown@brokage.com', 'treebeard.ent@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Kingdom of Men'
    FROM customers b, customers c
    WHERE b.email = 'aragorn.elessar@brokage.com'
      AND c.email IN ('faramir.gondor@brokage.com', 'eowyn.rohan@brokage.com', 'eomer.rohan@brokage.com',
                      'theoden.king@brokage.com', 'thorin.oakenshield@brokage.com', 'galadriel.lorien@brokage.com',
                      'elrond.rivendell@brokage.com', 'arwen.evenstar@brokage.com', 'saruman.white@brokage.com', 'gollum.smeagol@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Freed minds of Zion'
    FROM customers b, customers c
    WHERE b.email = 'morpheus.ship@brokage.com'
      AND c.email IN ('neo.anderson@brokage.com', 'trinity.matrix@brokage.com', 'agent.smith@brokage.com',
                      'oracle.cookie@brokage.com', 'niobe.zion@brokage.com', 'cypher.reagan@brokage.com',
                      'tank.operator@brokage.com', 'mouse.programmer@brokage.com', 'merovingian.french@brokage.com', 'seraph.guardian@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Gryffindor House'
    FROM customers b, customers c
    WHERE b.email = 'albus.dumbledore@brokage.com'
      AND c.email IN ('harry.potter@brokage.com', 'hermione.granger@brokage.com', 'ron.weasley@brokage.com',
                      'ginny.weasley@brokage.com', 'fred.weasley@brokage.com', 'george.weasley@brokage.com',
                      'neville.longbottom@brokage.com', 'luna.lovegood@brokage.com', 'rubeus.hagrid@brokage.com',
                      'sirius.black@brokage.com', 'remus.lupin@brokage.com', 'nymphadora.tonks@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Slytherin and others'
    FROM customers b, customers c
    WHERE b.email = 'severus.snape@brokage.com'
      AND c.email IN ('draco.malfoy@brokage.com', 'lucius.malfoy@brokage.com', 'minerva.mcgonagall@brokage.com',
                      'bellatrix.lestrange@brokage.com', 'voldemort.riddle@brokage.com', 'dobby.house@brokage.com',
                      'cedric.diggory@brokage.com', 'cho.chang@brokage.com', 'viktor.krum@brokage.com',
                      'fleur.delacour@brokage.com', 'molly.weasley@brokage.com', 'arthur.weasley@brokage.com', 'horace.slughorn@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Justice League members'
    FROM customers b, customers c
    WHERE b.email = 'bruce.wayne@brokage.com'
      AND c.email IN ('barry.allen@brokage.com', 'arthur.curry@brokage.com', 'victor.stone@brokage.com',
                      'hal.jordan@brokage.com', 'oliver.queen@brokage.com', 'dinah.lance@brokage.com',
                      'john.constantine@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Amazon allies'
    FROM customers b, customers c
    WHERE b.email = 'diana.prince@brokage.com'
      AND c.email IN ('zatanna.zatara@brokage.com', 'jonn.jonzz@brokage.com', 'shayera.hol@brokage.com',
                      'alfred.pennyworth@brokage.com', 'lois.lane@brokage.com', 'jimmy.olsen@brokage.com', 'selina.kyle@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    INSERT INTO broker_customers (broker_id, customer_id, active, notes)
    SELECT b.id, c.id, true, 'Villain rehabilitation program'
    FROM customers b, customers c
    WHERE b.email = 'clark.kent@brokage.com'
      AND c.email IN ('lex.luthor@brokage.com', 'joker.clown@brokage.com', 'harley.quinn@brokage.com',
                      'oswald.cobblepot@brokage.com', 'edward.nigma@brokage.com', 'pamela.isley@brokage.com')
    ON CONFLICT (broker_id, customer_id) DO NOTHING;

    -- =========================================================================
    -- CUSTOMER ASSETS (TRY balances and stock holdings)
    -- =========================================================================

    -- Give all CUSTOMERS TRY balance based on tier
    -- VIP: 1,000,000 TRY, PREMIUM: 500,000 TRY, STANDARD: 100,000 TRY
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'TRY',
           CASE c.tier
               WHEN 'VIP' THEN 1000000.0000
               WHEN 'PREMIUM' THEN 500000.0000
               ELSE 100000.0000
           END,
           CASE c.tier
               WHEN 'VIP' THEN 1000000.0000
               WHEN 'PREMIUM' THEN 500000.0000
               ELSE 100000.0000
           END
    FROM customers c
    WHERE c.role = 'CUSTOMER'
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Give some VIP customers stock holdings
    -- Tony Stark's customers get THYAO (aviation)
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'THYAO', 1000.0000, 1000.0000
    FROM customers c
    WHERE c.email IN ('thor.odinson@brokage.com', 'tchalla.wakanda@brokage.com', 'stephen.strange@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Rich characters get bank stocks
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'GARAN', 500.0000, 500.0000
    FROM customers c
    WHERE c.email IN ('pepper.potts@brokage.com', 'thanos.titan@brokage.com', 'draco.malfoy@brokage.com',
                      'lucius.malfoy@brokage.com', 'lex.luthor@brokage.com', 'oswald.cobblepot@brokage.com',
                      'bruce.wayne@brokage.com', 'oliver.queen@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Tech characters get TCELL
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'TCELL', 750.0000, 750.0000
    FROM customers c
    WHERE c.email IN ('shuri.wakanda@brokage.com', 'vision.synthezoid@brokage.com', 'neo.anderson@brokage.com',
                      'victor.stone@brokage.com', 'hermione.granger@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Warriors get ASELS (defense)
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'ASELS', 300.0000, 300.0000
    FROM customers c
    WHERE c.email IN ('natasha.romanoff@brokage.com', 'bucky.barnes@brokage.com', 'gamora.zen@brokage.com',
                      'legolas.greenleaf@brokage.com', 'gimli.gloin@brokage.com', 'eowyn.rohan@brokage.com',
                      'agent.smith@brokage.com', 'harry.potter@brokage.com', 'barry.allen@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Hobbits/small characters get BIMAS (retail - they love food)
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'BIMAS', 200.0000, 200.0000
    FROM customers c
    WHERE c.email IN ('frodo.baggins@brokage.com', 'samwise.gamgee@brokage.com', 'bilbo.baggins@brokage.com',
                      'merry.brandybuck@brokage.com', 'pippin.took@brokage.com', 'rocket.raccoon@brokage.com',
                      'dobby.house@brokage.com', 'ron.weasley@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- Kings/Leaders get KCHOL (holding)
    INSERT INTO customer_assets (customer_id, asset_name, size, usable_size)
    SELECT c.id, 'KCHOL', 1500.0000, 1500.0000
    FROM customers c
    WHERE c.email IN ('theoden.king@brokage.com', 'thorin.oakenshield@brokage.com', 'galadriel.lorien@brokage.com',
                      'elrond.rivendell@brokage.com', 'arthur.curry@brokage.com', 'jonn.jonzz@brokage.com')
    ON CONFLICT (customer_id, asset_name) DO NOTHING;

    -- =========================================================================
    -- ORDERS - Various states for testing
    -- =========================================================================

    -- PENDING BUY ORDERS (customers want to buy stocks)
    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'THYAO', 'BUY', 'LIMIT', 100.0000, 250.0000, 'PENDING', NOW() - INTERVAL '1 day'
    FROM customers c WHERE c.email = 'peter.parker@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'GARAN', 'BUY', 'LIMIT', 50.0000, 45.0000, 'PENDING', NOW() - INTERVAL '2 days'
    FROM customers c WHERE c.email = 'frodo.baggins@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'TCELL', 'BUY', 'LIMIT', 200.0000, 55.0000, 'PENDING', NOW() - INTERVAL '3 hours'
    FROM customers c WHERE c.email = 'neo.anderson@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'ASELS', 'BUY', 'LIMIT', 75.0000, 120.0000, 'PENDING', NOW() - INTERVAL '5 hours'
    FROM customers c WHERE c.email = 'harry.potter@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'KCHOL', 'BUY', 'LIMIT', 30.0000, 180.0000, 'PENDING', NOW() - INTERVAL '1 hour'
    FROM customers c WHERE c.email = 'barry.allen@brokage.com'
    ON CONFLICT DO NOTHING;

    -- PENDING SELL ORDERS (customers want to sell stocks they own)
    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'THYAO', 'SELL', 'LIMIT', 200.0000, 260.0000, 'PENDING', NOW() - INTERVAL '6 hours'
    FROM customers c WHERE c.email = 'thor.odinson@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'GARAN', 'SELL', 'LIMIT', 100.0000, 48.0000, 'PENDING', NOW() - INTERVAL '12 hours'
    FROM customers c WHERE c.email = 'pepper.potts@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'BIMAS', 'SELL', 'LIMIT', 50.0000, 95.0000, 'PENDING', NOW() - INTERVAL '4 hours'
    FROM customers c WHERE c.email = 'samwise.gamgee@brokage.com'
    ON CONFLICT DO NOTHING;

    -- MATCHED ORDERS (completed trades)
    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, filled_size, status, matched_at, created_at)
    SELECT c.id, 'THYAO', 'BUY', 'LIMIT', 500.0000, 240.0000, 500.0000, 'MATCHED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days'
    FROM customers c WHERE c.email = 'tchalla.wakanda@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, filled_size, status, matched_at, created_at)
    SELECT c.id, 'GARAN', 'BUY', 'LIMIT', 300.0000, 42.0000, 300.0000, 'MATCHED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '6 days'
    FROM customers c WHERE c.email = 'lex.luthor@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, filled_size, status, matched_at, created_at)
    SELECT c.id, 'TCELL', 'BUY', 'LIMIT', 250.0000, 50.0000, 250.0000, 'MATCHED', NOW() - INTERVAL '1 week', NOW() - INTERVAL '8 days'
    FROM customers c WHERE c.email = 'hermione.granger@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, filled_size, status, matched_at, created_at)
    SELECT c.id, 'ASELS', 'SELL', 'LIMIT', 100.0000, 115.0000, 100.0000, 'MATCHED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '5 days'
    FROM customers c WHERE c.email = 'natasha.romanoff@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, filled_size, status, matched_at, created_at)
    SELECT c.id, 'KCHOL', 'BUY', 'LIMIT', 1000.0000, 170.0000, 1000.0000, 'MATCHED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '12 days'
    FROM customers c WHERE c.email = 'galadriel.lorien@brokage.com'
    ON CONFLICT DO NOTHING;

    -- CANCELED ORDERS
    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, cancelled_at, created_at)
    SELECT c.id, 'THYAO', 'BUY', 'LIMIT', 1000.0000, 200.0000, 'CANCELED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '3 days'
    FROM customers c WHERE c.email = 'loki.laufeyson@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, cancelled_at, created_at)
    SELECT c.id, 'GARAN', 'SELL', 'LIMIT', 200.0000, 50.0000, 'CANCELED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '4 days'
    FROM customers c WHERE c.email = 'draco.malfoy@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, cancelled_at, created_at)
    SELECT c.id, 'TCELL', 'BUY', 'LIMIT', 500.0000, 60.0000, 'CANCELED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '7 days'
    FROM customers c WHERE c.email = 'agent.smith@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, cancelled_at, created_at)
    SELECT c.id, 'BIMAS', 'BUY', 'LIMIT', 100.0000, 100.0000, 'CANCELED', NOW() - INTERVAL '3 days', NOW() - INTERVAL '5 days'
    FROM customers c WHERE c.email = 'joker.clown@brokage.com'
    ON CONFLICT DO NOTHING;

    -- More diverse orders for testing date ranges
    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'EREGL', 'BUY', 'LIMIT', 150.0000, 85.0000, 'PENDING', NOW() - INTERVAL '30 minutes'
    FROM customers c WHERE c.email = 'bruce.banner@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'TUPRS', 'BUY', 'LIMIT', 80.0000, 150.0000, 'PENDING', NOW() - INTERVAL '45 minutes'
    FROM customers c WHERE c.email = 'carol.danvers@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'SISE', 'BUY', 'LIMIT', 200.0000, 35.0000, 'PENDING', NOW() - INTERVAL '2 hours'
    FROM customers c WHERE c.email = 'wanda.maximoff@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'SAHOL', 'BUY', 'LIMIT', 100.0000, 75.0000, 'PENDING', NOW() - INTERVAL '90 minutes'
    FROM customers c WHERE c.email = 'trinity.matrix@brokage.com'
    ON CONFLICT DO NOTHING;

    INSERT INTO orders (customer_id, asset_name, order_side, order_type, size, price, status, created_at)
    SELECT c.id, 'PGSUS', 'BUY', 'LIMIT', 120.0000, 280.0000, 'PENDING', NOW() - INTERVAL '20 minutes'
    FROM customers c WHERE c.email = 'diana.prince@brokage.com'
    ON CONFLICT DO NOTHING;

    -- Update usable_size for customers with PENDING BUY orders (TRY reserved)
    -- Peter Parker: 100 * 250 = 25,000 TRY reserved
    UPDATE customer_assets SET usable_size = size - 25000.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'peter.parker@brokage.com')
      AND asset_name = 'TRY';

    -- Frodo Baggins: 50 * 45 = 2,250 TRY reserved
    UPDATE customer_assets SET usable_size = size - 2250.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'frodo.baggins@brokage.com')
      AND asset_name = 'TRY';

    -- Neo Anderson: 200 * 55 = 11,000 TRY reserved
    UPDATE customer_assets SET usable_size = size - 11000.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'neo.anderson@brokage.com')
      AND asset_name = 'TRY';

    -- Harry Potter: 75 * 120 = 9,000 TRY reserved
    UPDATE customer_assets SET usable_size = size - 9000.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'harry.potter@brokage.com')
      AND asset_name = 'TRY';

    -- Barry Allen: 30 * 180 = 5,400 TRY reserved
    UPDATE customer_assets SET usable_size = size - 5400.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'barry.allen@brokage.com')
      AND asset_name = 'TRY';

    -- Update usable_size for customers with PENDING SELL orders (stocks reserved)
    -- Thor: 200 THYAO reserved
    UPDATE customer_assets SET usable_size = size - 200.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'thor.odinson@brokage.com')
      AND asset_name = 'THYAO';

    -- Pepper: 100 GARAN reserved
    UPDATE customer_assets SET usable_size = size - 100.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'pepper.potts@brokage.com')
      AND asset_name = 'GARAN';

    -- Sam: 50 BIMAS reserved
    UPDATE customer_assets SET usable_size = size - 50.0000
    WHERE customer_id = (SELECT id FROM customers WHERE email = 'samwise.gamgee@brokage.com')
      AND asset_name = 'BIMAS';

EOSQL

echo "Database initialization with comprehensive seed data completed successfully!"
echo "Created: 1 Admin, 10 Brokers, 100 Customers"
echo "With: Assets, Orders (PENDING/MATCHED/CANCELED), Broker-Customer assignments"
