// =============================================================================
// MongoDB - Database Initialization + Comprehensive Demo Seed Data
// =============================================================================
// Characters from: Marvel, Lord of the Rings, Hobbit, Matrix, Harry Potter, Justice League
//
// This script creates:
//   - Audit logs for order lifecycle, asset operations, customer actions
//   - Notifications for order confirmations, trade settlements, account updates
// =============================================================================

// Switch to admin database for authentication
db = db.getSiblingDB('admin');

// Create application databases and users
const databases = [
    { name: 'brokage_notifications', collections: ['notifications', 'templates', 'preferences'] },
    { name: 'brokage_audit', collections: ['audit_logs', 'notification_logs', 'telemetry', 'compliance'] }
];

databases.forEach(function(dbConfig) {
    print('Creating database: ' + dbConfig.name);

    // Switch to the database
    db = db.getSiblingDB(dbConfig.name);

    // Create collections with indexes
    dbConfig.collections.forEach(function(collName) {
        db.createCollection(collName);
        print('  Created collection: ' + collName);
    });
});

// =============================================================================
// INDEXES - Notifications Database
// =============================================================================
db = db.getSiblingDB('brokage_notifications');

db.notifications.createIndex({ "customerId": 1, "createdAt": -1 });
db.notifications.createIndex({ "customerId": 1, "status": 1 });
db.notifications.createIndex({ "notificationType": 1, "createdAt": -1 });
db.notifications.createIndex({ "channel": 1, "status": 1 });
db.notifications.createIndex({ "status": 1, "scheduledAt": 1 });
db.notifications.createIndex({ "eventId": 1 }, { unique: true });

print('Notifications indexes created');

// =============================================================================
// INDEXES - Audit Database
// =============================================================================
db = db.getSiblingDB('brokage_audit');

db.audit_logs.createIndex({ "timestamp": -1 });
db.audit_logs.createIndex({ "customerId": 1, "timestamp": -1 });
db.audit_logs.createIndex({ "entityType": 1, "entityId": 1 });
db.audit_logs.createIndex({ "entityType": 1, "action": 1, "timestamp": -1 });
db.audit_logs.createIndex({ "performedBy": 1, "timestamp": -1 });
db.audit_logs.createIndex({ "traceId": 1 });
db.audit_logs.createIndex({ "eventId": 1 }, { unique: true });

db.notification_logs.createIndex({ "customerId": 1, "timestamp": -1 });
db.notification_logs.createIndex({ "notificationType": 1, "timestamp": -1 });
db.notification_logs.createIndex({ "eventId": 1 }, { unique: true });

db.telemetry.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 604800 }); // 7 days TTL

print('Audit indexes created');

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function pastDate(daysAgo, hoursAgo) {
    var d = new Date();
    d.setDate(d.getDate() - (daysAgo || 0));
    d.setHours(d.getHours() - (hoursAgo || 0));
    return d;
}

function randomIP() {
    return '192.168.' + Math.floor(Math.random() * 255) + '.' + Math.floor(Math.random() * 255);
}

// =============================================================================
// DEMO DATA - Customer Reference (matches PostgreSQL seed)
// =============================================================================

// These are sample UUIDs for demo - in real system, would be synced with PostgreSQL
var customers = {
    // Admin
    'nick.fury': { id: '00000001-0001-0001-0001-000000000001', email: 'nick.fury@brokage.com', name: 'Nick Fury', role: 'ADMIN' },

    // Brokers
    'tony.stark': { id: '00000002-0001-0001-0001-000000000001', email: 'tony.stark@brokage.com', name: 'Tony Stark', role: 'BROKER' },
    'steve.rogers': { id: '00000002-0001-0001-0001-000000000002', email: 'steve.rogers@brokage.com', name: 'Steve Rogers', role: 'BROKER' },
    'gandalf.grey': { id: '00000002-0001-0001-0001-000000000003', email: 'gandalf.grey@brokage.com', name: 'Gandalf Grey', role: 'BROKER' },
    'morpheus.ship': { id: '00000002-0001-0001-0001-000000000005', email: 'morpheus.ship@brokage.com', name: 'Morpheus Ship', role: 'BROKER' },
    'albus.dumbledore': { id: '00000002-0001-0001-0001-000000000006', email: 'albus.dumbledore@brokage.com', name: 'Albus Dumbledore', role: 'BROKER' },
    'bruce.wayne': { id: '00000002-0001-0001-0001-000000000008', email: 'bruce.wayne@brokage.com', name: 'Bruce Wayne', role: 'BROKER' },

    // Marvel Customers
    'peter.parker': { id: '00000003-0001-0001-0001-000000000001', email: 'peter.parker@brokage.com', name: 'Peter Parker', role: 'CUSTOMER' },
    'thor.odinson': { id: '00000003-0001-0001-0001-000000000003', email: 'thor.odinson@brokage.com', name: 'Thor Odinson', role: 'CUSTOMER' },
    'natasha.romanoff': { id: '00000003-0001-0001-0001-000000000004', email: 'natasha.romanoff@brokage.com', name: 'Natasha Romanoff', role: 'CUSTOMER' },
    'pepper.potts': { id: '00000003-0001-0001-0001-000000000021', email: 'pepper.potts@brokage.com', name: 'Pepper Potts', role: 'CUSTOMER' },
    'thanos.titan': { id: '00000003-0001-0001-0001-000000000024', email: 'thanos.titan@brokage.com', name: 'Thanos Titan', role: 'CUSTOMER' },

    // LOTR Customers
    'frodo.baggins': { id: '00000003-0001-0001-0002-000000000001', email: 'frodo.baggins@brokage.com', name: 'Frodo Baggins', role: 'CUSTOMER' },
    'samwise.gamgee': { id: '00000003-0001-0001-0002-000000000002', email: 'samwise.gamgee@brokage.com', name: 'Samwise Gamgee', role: 'CUSTOMER' },
    'legolas.greenleaf': { id: '00000003-0001-0001-0002-000000000003', email: 'legolas.greenleaf@brokage.com', name: 'Legolas Greenleaf', role: 'CUSTOMER' },

    // Matrix Customers
    'neo.anderson': { id: '00000003-0001-0001-0003-000000000001', email: 'neo.anderson@brokage.com', name: 'Neo Anderson', role: 'CUSTOMER' },
    'trinity.matrix': { id: '00000003-0001-0001-0003-000000000002', email: 'trinity.matrix@brokage.com', name: 'Trinity Matrix', role: 'CUSTOMER' },
    'agent.smith': { id: '00000003-0001-0001-0003-000000000003', email: 'agent.smith@brokage.com', name: 'Agent Smith', role: 'CUSTOMER' },

    // Harry Potter Customers
    'harry.potter': { id: '00000003-0001-0001-0004-000000000001', email: 'harry.potter@brokage.com', name: 'Harry Potter', role: 'CUSTOMER' },
    'hermione.granger': { id: '00000003-0001-0001-0004-000000000002', email: 'hermione.granger@brokage.com', name: 'Hermione Granger', role: 'CUSTOMER' },
    'draco.malfoy': { id: '00000003-0001-0001-0004-000000000009', email: 'draco.malfoy@brokage.com', name: 'Draco Malfoy', role: 'CUSTOMER' },

    // Justice League Customers
    'barry.allen': { id: '00000003-0001-0001-0005-000000000001', email: 'barry.allen@brokage.com', name: 'Barry Allen', role: 'CUSTOMER' },
    'arthur.curry': { id: '00000003-0001-0001-0005-000000000002', email: 'arthur.curry@brokage.com', name: 'Arthur Curry', role: 'CUSTOMER' },
    'lex.luthor': { id: '00000003-0001-0001-0005-000000000014', email: 'lex.luthor@brokage.com', name: 'Lex Luthor', role: 'CUSTOMER' }
};

// Sample Order IDs (would match PostgreSQL orders)
var orders = {
    'order1': generateUUID(),
    'order2': generateUUID(),
    'order3': generateUUID(),
    'order4': generateUUID(),
    'order5': generateUUID(),
    'order6': generateUUID(),
    'order7': generateUUID(),
    'order8': generateUUID(),
    'order9': generateUUID(),
    'order10': generateUUID()
};

// =============================================================================
// SEED AUDIT LOGS
// =============================================================================
db = db.getSiblingDB('brokage_audit');

print('Seeding audit_logs collection...');

var auditLogs = [
    // =========================================================================
    // ORDER CREATED EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order1,
        action: 'ORDER_CREATED',
        customerId: customers['peter.parker'].id,
        customerEmail: customers['peter.parker'].email,
        performedBy: customers['peter.parker'].id,
        performedByEmail: customers['peter.parker'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order1,
            assetName: 'THYAO',
            orderSide: 'BUY',
            size: 100,
            price: 250,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '192.168.1.101',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Peter Parker placed a BUY order for 100 THYAO at 250 TRY',
        timestamp: pastDate(1, 0),
        createdAt: pastDate(1, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order2,
        action: 'ORDER_CREATED',
        customerId: customers['frodo.baggins'].id,
        customerEmail: customers['frodo.baggins'].email,
        performedBy: customers['frodo.baggins'].id,
        performedByEmail: customers['frodo.baggins'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order2,
            assetName: 'GARAN',
            orderSide: 'BUY',
            size: 50,
            price: 45,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '192.168.1.102',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Frodo Baggins placed a BUY order for 50 GARAN at 45 TRY',
        timestamp: pastDate(2, 0),
        createdAt: pastDate(2, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order3,
        action: 'ORDER_CREATED',
        customerId: customers['neo.anderson'].id,
        customerEmail: customers['neo.anderson'].email,
        performedBy: customers['neo.anderson'].id,
        performedByEmail: customers['neo.anderson'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order3,
            assetName: 'TCELL',
            orderSide: 'BUY',
            size: 200,
            price: 55,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '10.0.0.42',
        userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Neo chose to BUY 200 TCELL at 55 TRY - There is no spoon, only stocks',
        timestamp: pastDate(0, 3),
        createdAt: pastDate(0, 3)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order4,
        action: 'ORDER_CREATED',
        customerId: customers['harry.potter'].id,
        customerEmail: customers['harry.potter'].email,
        performedBy: customers['harry.potter'].id,
        performedByEmail: customers['harry.potter'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order4,
            assetName: 'ASELS',
            orderSide: 'BUY',
            size: 75,
            price: 120,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '192.168.7.31',
        userAgent: 'Mozilla/5.0 (Magical; Hogwarts Express) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Harry Potter placed a BUY order for 75 ASELS (Defense stocks, naturally)',
        timestamp: pastDate(0, 5),
        createdAt: pastDate(0, 5)
    },

    // =========================================================================
    // SELL ORDER CREATED
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order5,
        action: 'ORDER_CREATED',
        customerId: customers['thor.odinson'].id,
        customerEmail: customers['thor.odinson'].email,
        performedBy: customers['thor.odinson'].id,
        performedByEmail: customers['thor.odinson'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order5,
            assetName: 'THYAO',
            orderSide: 'SELL',
            size: 200,
            price: 260,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '192.168.9.1',
        userAgent: 'Mozilla/5.0 (Asgard; Thunder God) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Thor Odinson placed a SELL order for 200 THYAO - Flying is overrated when you have Mjolnir',
        timestamp: pastDate(0, 6),
        createdAt: pastDate(0, 6)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order6,
        action: 'ORDER_CREATED',
        customerId: customers['pepper.potts'].id,
        customerEmail: customers['pepper.potts'].email,
        performedBy: customers['pepper.potts'].id,
        performedByEmail: customers['pepper.potts'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: {
            orderId: orders.order6,
            assetName: 'GARAN',
            orderSide: 'SELL',
            size: 100,
            price: 48,
            status: 'PENDING'
        },
        changes: { status: { from: null, to: 'PENDING' } },
        ipAddress: '10.0.0.1',
        userAgent: 'Mozilla/5.0 (Stark Industries; CEO Edition) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Pepper Potts placed a SELL order for 100 GARAN - Diversifying Stark Industries portfolio',
        timestamp: pastDate(0, 12),
        createdAt: pastDate(0, 12)
    },

    // =========================================================================
    // ORDER MATCHED EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order7,
        action: 'ORDER_MATCHED',
        customerId: customers['thanos.titan'].id,
        customerEmail: customers['thanos.titan'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: {
            orderId: orders.order7,
            assetName: 'KCHOL',
            orderSide: 'BUY',
            size: 500,
            price: 175,
            status: 'PENDING'
        },
        newState: {
            orderId: orders.order7,
            assetName: 'KCHOL',
            orderSide: 'BUY',
            size: 500,
            price: 175,
            filledSize: 500,
            status: 'MATCHED'
        },
        changes: { status: { from: 'PENDING', to: 'MATCHED' }, filledSize: { from: 0, to: 500 } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Director Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-processor',
        description: 'Admin Nick Fury matched Thanos BUY order - Inevitable trade execution',
        timestamp: pastDate(3, 0),
        createdAt: pastDate(3, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order8,
        action: 'ORDER_MATCHED',
        customerId: customers['hermione.granger'].id,
        customerEmail: customers['hermione.granger'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: {
            orderId: orders.order8,
            assetName: 'TCELL',
            orderSide: 'BUY',
            size: 300,
            price: 52,
            status: 'PENDING'
        },
        newState: {
            orderId: orders.order8,
            assetName: 'TCELL',
            orderSide: 'BUY',
            size: 300,
            price: 52,
            filledSize: 300,
            status: 'MATCHED'
        },
        changes: { status: { from: 'PENDING', to: 'MATCHED' }, filledSize: { from: 0, to: 300 } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Director Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-processor',
        description: 'Hermione Granger TCELL order matched - Books and tech stocks, a perfect combination',
        timestamp: pastDate(4, 0),
        createdAt: pastDate(4, 0)
    },

    // =========================================================================
    // ORDER CANCELED EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order9,
        action: 'ORDER_CANCELED',
        customerId: customers['agent.smith'].id,
        customerEmail: customers['agent.smith'].email,
        performedBy: customers['agent.smith'].id,
        performedByEmail: customers['agent.smith'].email,
        performedByRole: 'CUSTOMER',
        previousState: {
            orderId: orders.order9,
            assetName: 'ASELS',
            orderSide: 'BUY',
            size: 1000,
            price: 115,
            status: 'PENDING'
        },
        newState: {
            orderId: orders.order9,
            assetName: 'ASELS',
            orderSide: 'BUY',
            size: 1000,
            price: 115,
            status: 'CANCELED'
        },
        changes: { status: { from: 'PENDING', to: 'CANCELED' } },
        ipAddress: '10.255.255.1',
        userAgent: 'Mozilla/5.0 (Matrix; Agent Program v1.0) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Agent Smith cancelled BUY order - Purpose changed, new directive received',
        timestamp: pastDate(5, 0),
        createdAt: pastDate(5, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Order',
        entityId: orders.order10,
        action: 'ORDER_CANCELED',
        customerId: customers['draco.malfoy'].id,
        customerEmail: customers['draco.malfoy'].email,
        performedBy: customers['draco.malfoy'].id,
        performedByEmail: customers['draco.malfoy'].email,
        performedByRole: 'CUSTOMER',
        previousState: {
            orderId: orders.order10,
            assetName: 'GARAN',
            orderSide: 'SELL',
            size: 200,
            price: 42,
            status: 'PENDING'
        },
        newState: {
            orderId: orders.order10,
            assetName: 'GARAN',
            orderSide: 'SELL',
            size: 200,
            price: 42,
            status: 'CANCELED'
        },
        changes: { status: { from: 'PENDING', to: 'CANCELED' } },
        ipAddress: '192.168.7.77',
        userAgent: 'Mozilla/5.0 (Slytherin; Pure-blood Edition) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'order-service',
        description: 'Draco Malfoy cancelled SELL order - Father will hear about this price drop',
        timestamp: pastDate(6, 0),
        createdAt: pastDate(6, 0)
    },

    // =========================================================================
    // ASSET DEPOSIT EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Asset',
        entityId: generateUUID(),
        action: 'ASSET_DEPOSITED',
        customerId: customers['lex.luthor'].id,
        customerEmail: customers['lex.luthor'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: { assetName: 'TRY', size: 500000, usableSize: 500000 },
        newState: { assetName: 'TRY', size: 1500000, usableSize: 1500000 },
        changes: { size: { from: 500000, to: 1500000 }, usableSize: { from: 500000, to: 1500000 } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Admin Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'asset-service',
        description: 'Lex Luthor deposited 1,000,000 TRY - LexCorp quarterly dividend reinvestment',
        timestamp: pastDate(7, 0),
        createdAt: pastDate(7, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Asset',
        entityId: generateUUID(),
        action: 'ASSET_DEPOSITED',
        customerId: customers['arthur.curry'].id,
        customerEmail: customers['arthur.curry'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: { assetName: 'TRY', size: 1000000, usableSize: 1000000 },
        newState: { assetName: 'TRY', size: 2000000, usableSize: 2000000 },
        changes: { size: { from: 1000000, to: 2000000 }, usableSize: { from: 1000000, to: 2000000 } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Admin Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'asset-service',
        description: 'Arthur Curry deposited 1,000,000 TRY - Atlantean treasury transfer',
        timestamp: pastDate(8, 0),
        createdAt: pastDate(8, 0)
    },

    // =========================================================================
    // ASSET WITHDRAWAL EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Asset',
        entityId: generateUUID(),
        action: 'ASSET_WITHDRAWN',
        customerId: customers['legolas.greenleaf'].id,
        customerEmail: customers['legolas.greenleaf'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: { assetName: 'TRY', size: 500000, usableSize: 480000 },
        newState: { assetName: 'TRY', size: 400000, usableSize: 380000 },
        changes: { size: { from: 500000, to: 400000 }, usableSize: { from: 480000, to: 380000 } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Admin Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'asset-service',
        description: 'Legolas Greenleaf withdrew 100,000 TRY - Elvish bow upgrade fund',
        timestamp: pastDate(9, 0),
        createdAt: pastDate(9, 0)
    },

    // =========================================================================
    // CUSTOMER REGISTRATION EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Customer',
        entityId: customers['barry.allen'].id,
        action: 'CUSTOMER_REGISTERED',
        customerId: customers['barry.allen'].id,
        customerEmail: customers['barry.allen'].email,
        performedBy: customers['bruce.wayne'].id,
        performedByEmail: customers['bruce.wayne'].email,
        performedByRole: 'BROKER',
        previousState: null,
        newState: {
            firstName: 'Barry',
            lastName: 'Allen',
            email: 'barry.allen@brokage.com',
            tier: 'PREMIUM',
            status: 'ACTIVE'
        },
        changes: { status: { from: null, to: 'ACTIVE' } },
        ipAddress: '192.168.50.1',
        userAgent: 'Mozilla/5.0 (Wayne Enterprises; Broker Terminal) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'customer-service',
        description: 'Bruce Wayne registered Barry Allen as a new customer - Fast onboarding, naturally',
        timestamp: pastDate(30, 0),
        createdAt: pastDate(30, 0)
    },
    {
        eventId: generateUUID(),
        entityType: 'Customer',
        entityId: customers['trinity.matrix'].id,
        action: 'CUSTOMER_REGISTERED',
        customerId: customers['trinity.matrix'].id,
        customerEmail: customers['trinity.matrix'].email,
        performedBy: customers['morpheus.ship'].id,
        performedByEmail: customers['morpheus.ship'].email,
        performedByRole: 'BROKER',
        previousState: null,
        newState: {
            firstName: 'Trinity',
            lastName: 'Matrix',
            email: 'trinity.matrix@brokage.com',
            tier: 'PREMIUM',
            status: 'ACTIVE'
        },
        changes: { status: { from: null, to: 'ACTIVE' } },
        ipAddress: '10.0.0.99',
        userAgent: 'Mozilla/5.0 (Nebuchadnezzar; Operator Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'customer-service',
        description: 'Morpheus registered Trinity - She can dodge market crashes like bullets',
        timestamp: pastDate(45, 0),
        createdAt: pastDate(45, 0)
    },

    // =========================================================================
    // BROKER ASSIGNMENT EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'BrokerCustomer',
        entityId: generateUUID(),
        action: 'BROKER_ASSIGNED',
        customerId: customers['peter.parker'].id,
        customerEmail: customers['peter.parker'].email,
        performedBy: customers['nick.fury'].id,
        performedByEmail: customers['nick.fury'].email,
        performedByRole: 'ADMIN',
        previousState: null,
        newState: {
            brokerId: customers['tony.stark'].id,
            brokerEmail: customers['tony.stark'].email,
            customerId: customers['peter.parker'].id,
            notes: 'Stark Industries internship program'
        },
        changes: { brokerId: { from: null, to: customers['tony.stark'].id } },
        ipAddress: '192.168.100.1',
        userAgent: 'Mozilla/5.0 (SHIELD HQ; Director Console) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'customer-service',
        description: 'Peter Parker assigned to broker Tony Stark - With great portfolio comes great responsibility',
        timestamp: pastDate(60, 0),
        createdAt: pastDate(60, 0)
    },

    // =========================================================================
    // LOGIN EVENTS
    // =========================================================================
    {
        eventId: generateUUID(),
        entityType: 'Session',
        entityId: generateUUID(),
        action: 'USER_LOGIN',
        customerId: customers['neo.anderson'].id,
        customerEmail: customers['neo.anderson'].email,
        performedBy: customers['neo.anderson'].id,
        performedByEmail: customers['neo.anderson'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: { loginTime: pastDate(0, 2), device: 'Desktop', browser: 'Chrome' },
        changes: null,
        ipAddress: '10.0.0.42',
        userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'auth-service',
        description: 'Neo logged in - Entering the trading matrix',
        timestamp: pastDate(0, 2),
        createdAt: pastDate(0, 2)
    },
    {
        eventId: generateUUID(),
        entityType: 'Session',
        entityId: generateUUID(),
        action: 'USER_LOGIN',
        customerId: customers['hermione.granger'].id,
        customerEmail: customers['hermione.granger'].email,
        performedBy: customers['hermione.granger'].id,
        performedByEmail: customers['hermione.granger'].email,
        performedByRole: 'CUSTOMER',
        previousState: null,
        newState: { loginTime: pastDate(0, 4), device: 'Mobile', browser: 'Safari' },
        changes: null,
        ipAddress: '192.168.7.19',
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) AppleWebKit/605.1.15',
        requestId: generateUUID(),
        traceId: generateUUID().substring(0, 16),
        spanId: generateUUID().substring(0, 8),
        serviceName: 'auth-service',
        description: 'Hermione logged in on mobile - Even at Hogwarts, she monitors her investments',
        timestamp: pastDate(0, 4),
        createdAt: pastDate(0, 4)
    }
];

// Insert audit logs
db.audit_logs.insertMany(auditLogs);
print('Inserted ' + auditLogs.length + ' audit logs');

// =============================================================================
// SEED NOTIFICATIONS
// =============================================================================
db = db.getSiblingDB('brokage_notifications');

print('Seeding notifications collection...');

var notifications = [
    // =========================================================================
    // ORDER CONFIRMATION NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['peter.parker'].id,
        customerEmail: customers['peter.parker'].email,
        notificationType: 'ORDER_CREATED',
        channel: 'EMAIL',
        recipient: customers['peter.parker'].email,
        templateCode: 'ORDER_CONFIRMATION',
        subject: 'Order Confirmation - BUY 100 THYAO',
        body: 'Dear Peter Parker,\n\nYour order has been successfully placed.\n\nOrder Details:\n- Asset: THYAO\n- Side: BUY\n- Quantity: 100\n- Price: 250.00 TRY\n- Total Value: 25,000.00 TRY\n- Status: PENDING\n\nYou will be notified when your order is matched.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Peter Parker',
            assetName: 'THYAO',
            orderSide: 'BUY',
            size: 100,
            price: 250,
            totalValue: 25000
        },
        metadata: { orderId: orders.order1 },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(1, 0),
        createdAt: pastDate(1, 0),
        updatedAt: pastDate(1, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['peter.parker'].id,
        customerEmail: customers['peter.parker'].email,
        notificationType: 'ORDER_CREATED',
        channel: 'IN_APP',
        recipient: customers['peter.parker'].id,
        templateCode: 'ORDER_CONFIRMATION_INAPP',
        subject: 'Order Placed',
        body: 'Your BUY order for 100 THYAO at 250.00 TRY has been placed successfully.',
        templateVariables: { assetName: 'THYAO', orderSide: 'BUY', size: 100, price: 250 },
        metadata: { orderId: orders.order1 },
        status: 'DELIVERED',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(1, 0),
        deliveredAt: pastDate(1, 0),
        createdAt: pastDate(1, 0),
        updatedAt: pastDate(1, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['frodo.baggins'].id,
        customerEmail: customers['frodo.baggins'].email,
        notificationType: 'ORDER_CREATED',
        channel: 'EMAIL',
        recipient: customers['frodo.baggins'].email,
        templateCode: 'ORDER_CONFIRMATION',
        subject: 'Order Confirmation - BUY 50 GARAN',
        body: 'Dear Frodo Baggins,\n\nYour order has been successfully placed.\n\nOrder Details:\n- Asset: GARAN\n- Side: BUY\n- Quantity: 50\n- Price: 45.00 TRY\n- Total Value: 2,250.00 TRY\n- Status: PENDING\n\nOne does not simply walk into a trade without confirmation!\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Frodo Baggins',
            assetName: 'GARAN',
            orderSide: 'BUY',
            size: 50,
            price: 45,
            totalValue: 2250
        },
        metadata: { orderId: orders.order2 },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(2, 0),
        createdAt: pastDate(2, 0),
        updatedAt: pastDate(2, 0)
    },

    // =========================================================================
    // ORDER MATCHED NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['thanos.titan'].id,
        customerEmail: customers['thanos.titan'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'EMAIL',
        recipient: customers['thanos.titan'].email,
        templateCode: 'ORDER_MATCHED',
        subject: 'Trade Executed - BUY 500 KCHOL',
        body: 'Dear Thanos Titan,\n\nGreat news! Your order has been matched and executed.\n\nTrade Details:\n- Asset: KCHOL (Koc Holding)\n- Side: BUY\n- Quantity: 500\n- Execution Price: 175.00 TRY\n- Total Value: 87,500.00 TRY\n- Status: MATCHED\n\nThe trade was... inevitable.\n\nYour portfolio has been updated accordingly.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Thanos Titan',
            assetName: 'KCHOL',
            orderSide: 'BUY',
            size: 500,
            price: 175,
            totalValue: 87500
        },
        metadata: { orderId: orders.order7, matchedAt: pastDate(3, 0) },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(3, 0),
        createdAt: pastDate(3, 0),
        updatedAt: pastDate(3, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['hermione.granger'].id,
        customerEmail: customers['hermione.granger'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'EMAIL',
        recipient: customers['hermione.granger'].email,
        templateCode: 'ORDER_MATCHED',
        subject: 'Trade Executed - BUY 300 TCELL',
        body: 'Dear Hermione Granger,\n\nYour order has been matched and executed.\n\nTrade Details:\n- Asset: TCELL (Turkcell)\n- Side: BUY\n- Quantity: 300\n- Execution Price: 52.00 TRY\n- Total Value: 15,600.00 TRY\n- Status: MATCHED\n\nA brilliant investment choice, as expected!\n\nYour portfolio has been updated.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Hermione Granger',
            assetName: 'TCELL',
            orderSide: 'BUY',
            size: 300,
            price: 52,
            totalValue: 15600
        },
        metadata: { orderId: orders.order8 },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(4, 0),
        createdAt: pastDate(4, 0),
        updatedAt: pastDate(4, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['hermione.granger'].id,
        customerEmail: customers['hermione.granger'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'PUSH',
        recipient: 'device_token_hermione_001',
        templateCode: 'ORDER_MATCHED_PUSH',
        subject: 'Trade Executed',
        body: 'Your BUY order for 300 TCELL has been matched at 52.00 TRY!',
        templateVariables: { assetName: 'TCELL', size: 300, price: 52 },
        metadata: { orderId: orders.order8 },
        status: 'DELIVERED',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(4, 0),
        deliveredAt: pastDate(4, 0),
        createdAt: pastDate(4, 0),
        updatedAt: pastDate(4, 0)
    },

    // =========================================================================
    // ORDER CANCELED NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['agent.smith'].id,
        customerEmail: customers['agent.smith'].email,
        notificationType: 'ORDER_CANCELED',
        channel: 'EMAIL',
        recipient: customers['agent.smith'].email,
        templateCode: 'ORDER_CANCELED',
        subject: 'Order Cancelled - BUY 1000 ASELS',
        body: 'Dear Agent Smith,\n\nYour order has been cancelled as requested.\n\nCancelled Order Details:\n- Asset: ASELS\n- Side: BUY\n- Quantity: 1,000\n- Price: 115.00 TRY\n- Reserved Amount: 115,000.00 TRY (Released)\n\nYour TRY balance has been restored.\n\nPurpose... changed.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Agent Smith',
            assetName: 'ASELS',
            orderSide: 'BUY',
            size: 1000,
            price: 115,
            releasedAmount: 115000
        },
        metadata: { orderId: orders.order9 },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(5, 0),
        createdAt: pastDate(5, 0),
        updatedAt: pastDate(5, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['draco.malfoy'].id,
        customerEmail: customers['draco.malfoy'].email,
        notificationType: 'ORDER_CANCELED',
        channel: 'EMAIL',
        recipient: customers['draco.malfoy'].email,
        templateCode: 'ORDER_CANCELED',
        subject: 'Order Cancelled - SELL 200 GARAN',
        body: 'Dear Draco Malfoy,\n\nYour order has been cancelled as requested.\n\nCancelled Order Details:\n- Asset: GARAN\n- Side: SELL\n- Quantity: 200\n- Price: 42.00 TRY\n- Reserved Shares: 200 (Released)\n\nYour GARAN shares have been released back to your portfolio.\n\nFather will be informed of this decision.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Draco Malfoy',
            assetName: 'GARAN',
            orderSide: 'SELL',
            size: 200,
            price: 42
        },
        metadata: { orderId: orders.order10 },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(6, 0),
        createdAt: pastDate(6, 0),
        updatedAt: pastDate(6, 0)
    },

    // =========================================================================
    // DEPOSIT NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['lex.luthor'].id,
        customerEmail: customers['lex.luthor'].email,
        notificationType: 'DEPOSIT_CONFIRMED',
        channel: 'EMAIL',
        recipient: customers['lex.luthor'].email,
        templateCode: 'DEPOSIT_CONFIRMATION',
        subject: 'Deposit Confirmed - 1,000,000.00 TRY',
        body: 'Dear Lex Luthor,\n\nYour deposit has been confirmed and credited to your account.\n\nDeposit Details:\n- Amount: 1,000,000.00 TRY\n- Previous Balance: 500,000.00 TRY\n- New Balance: 1,500,000.00 TRY\n\nLexCorp quarterly dividend reinvestment successful.\n\nYou are now ready to execute your trading strategy.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Lex Luthor',
            amount: 1000000,
            previousBalance: 500000,
            newBalance: 1500000
        },
        metadata: { transactionType: 'DEPOSIT' },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(7, 0),
        createdAt: pastDate(7, 0),
        updatedAt: pastDate(7, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['arthur.curry'].id,
        customerEmail: customers['arthur.curry'].email,
        notificationType: 'DEPOSIT_CONFIRMED',
        channel: 'EMAIL',
        recipient: customers['arthur.curry'].email,
        templateCode: 'DEPOSIT_CONFIRMATION',
        subject: 'Deposit Confirmed - 1,000,000.00 TRY',
        body: 'Dear Arthur Curry,\n\nYour deposit has been confirmed and credited to your account.\n\nDeposit Details:\n- Amount: 1,000,000.00 TRY\n- Previous Balance: 1,000,000.00 TRY\n- New Balance: 2,000,000.00 TRY\n\nAtlantean treasury transfer completed.\n\nMay your trades flow like the ocean currents!\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Arthur Curry',
            amount: 1000000,
            previousBalance: 1000000,
            newBalance: 2000000
        },
        metadata: { transactionType: 'DEPOSIT' },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(8, 0),
        createdAt: pastDate(8, 0),
        updatedAt: pastDate(8, 0)
    },

    // =========================================================================
    // WITHDRAWAL NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['legolas.greenleaf'].id,
        customerEmail: customers['legolas.greenleaf'].email,
        notificationType: 'WITHDRAWAL_COMPLETED',
        channel: 'EMAIL',
        recipient: customers['legolas.greenleaf'].email,
        templateCode: 'WITHDRAWAL_CONFIRMATION',
        subject: 'Withdrawal Completed - 100,000.00 TRY',
        body: 'Dear Legolas Greenleaf,\n\nYour withdrawal request has been processed.\n\nWithdrawal Details:\n- Amount: 100,000.00 TRY\n- Previous Balance: 500,000.00 TRY\n- New Balance: 400,000.00 TRY\n\nFunds transferred for Elvish bow upgrade.\nMay your arrows always find their mark!\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Legolas Greenleaf',
            amount: 100000,
            previousBalance: 500000,
            newBalance: 400000
        },
        metadata: { transactionType: 'WITHDRAWAL' },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(9, 0),
        createdAt: pastDate(9, 0),
        updatedAt: pastDate(9, 0)
    },

    // =========================================================================
    // WELCOME NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['barry.allen'].id,
        customerEmail: customers['barry.allen'].email,
        notificationType: 'WELCOME',
        channel: 'EMAIL',
        recipient: customers['barry.allen'].email,
        templateCode: 'WELCOME_EMAIL',
        subject: 'Welcome to Brokage Trading Platform!',
        body: 'Dear Barry Allen,\n\nWelcome to Brokage Trading Platform!\n\nYou have been registered as a PREMIUM customer.\n\nYour assigned broker: Bruce Wayne\n\nQuick Start Guide:\n1. Deposit TRY to your account\n2. Browse available stocks in the market\n3. Place BUY or SELL orders\n4. Track your portfolio performance\n\nFast trading for fast heroes!\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Barry Allen',
            tier: 'PREMIUM',
            brokerName: 'Bruce Wayne'
        },
        metadata: { registrationDate: pastDate(30, 0) },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(30, 0),
        createdAt: pastDate(30, 0),
        updatedAt: pastDate(30, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['trinity.matrix'].id,
        customerEmail: customers['trinity.matrix'].email,
        notificationType: 'WELCOME',
        channel: 'EMAIL',
        recipient: customers['trinity.matrix'].email,
        templateCode: 'WELCOME_EMAIL',
        subject: 'Welcome to Brokage Trading Platform!',
        body: 'Dear Trinity,\n\nWelcome to Brokage Trading Platform!\n\nYou have been registered as a PREMIUM customer.\n\nYour assigned broker: Morpheus Ship\n\nQuick Start Guide:\n1. Deposit TRY to your account\n2. Browse available stocks in the market\n3. Place BUY or SELL orders\n4. Track your portfolio performance\n\nDodge the losses, embrace the gains.\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Trinity',
            tier: 'PREMIUM',
            brokerName: 'Morpheus Ship'
        },
        metadata: { registrationDate: pastDate(45, 0) },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(45, 0),
        createdAt: pastDate(45, 0),
        updatedAt: pastDate(45, 0)
    },

    // =========================================================================
    // MARKET ALERT NOTIFICATIONS
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['neo.anderson'].id,
        customerEmail: customers['neo.anderson'].email,
        notificationType: 'PRICE_ALERT',
        channel: 'PUSH',
        recipient: 'device_token_neo_001',
        templateCode: 'PRICE_ALERT_PUSH',
        subject: 'TCELL Price Alert',
        body: 'TCELL reached your target price of 55.00 TRY! Current: 55.25 TRY (+2.3%)',
        templateVariables: { assetName: 'TCELL', targetPrice: 55, currentPrice: 55.25, change: '+2.3%' },
        metadata: { alertType: 'PRICE_TARGET' },
        status: 'DELIVERED',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(0, 3),
        deliveredAt: pastDate(0, 3),
        createdAt: pastDate(0, 3),
        updatedAt: pastDate(0, 3)
    },
    {
        eventId: generateUUID(),
        customerId: customers['pepper.potts'].id,
        customerEmail: customers['pepper.potts'].email,
        notificationType: 'MARKET_CLOSE',
        channel: 'EMAIL',
        recipient: customers['pepper.potts'].email,
        templateCode: 'DAILY_SUMMARY',
        subject: 'Daily Portfolio Summary - December 24, 2024',
        body: 'Dear Pepper Potts,\n\nHere is your daily portfolio summary:\n\nPortfolio Value: 1,523,450.00 TRY (+1.2%)\n\nTop Performers:\n- GARAN: +2.5%\n- THYAO: +1.8%\n\nPending Orders: 1\n- SELL 100 GARAN at 48.00 TRY\n\nStark Industries portfolio remains strong!\n\nBest regards,\nBrokage Trading Platform',
        templateVariables: {
            customerName: 'Pepper Potts',
            portfolioValue: 1523450,
            dailyChange: '+1.2%',
            pendingOrders: 1
        },
        metadata: { reportDate: pastDate(1, 0) },
        status: 'SENT',
        retryCount: 0,
        maxRetries: 3,
        sentAt: pastDate(0, 18),
        createdAt: pastDate(0, 18),
        updatedAt: pastDate(0, 18)
    },

    // =========================================================================
    // FAILED NOTIFICATION (for testing retry mechanism)
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['thanos.titan'].id,
        customerEmail: customers['thanos.titan'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'SMS',
        recipient: '+905551234567',
        templateCode: 'ORDER_MATCHED_SMS',
        subject: null,
        body: 'Your KCHOL order matched! 500 shares at 175 TRY. Total: 87,500 TRY',
        templateVariables: { assetName: 'KCHOL', size: 500, price: 175, totalValue: 87500 },
        metadata: { orderId: orders.order7 },
        status: 'FAILED',
        errorMessage: 'SMS gateway timeout - Titan network interference',
        retryCount: 3,
        maxRetries: 3,
        failedAt: pastDate(3, 0),
        createdAt: pastDate(3, 0),
        updatedAt: pastDate(3, 0)
    },

    // =========================================================================
    // PENDING NOTIFICATIONS (scheduled)
    // =========================================================================
    {
        eventId: generateUUID(),
        customerId: customers['harry.potter'].id,
        customerEmail: customers['harry.potter'].email,
        notificationType: 'WEEKLY_REPORT',
        channel: 'EMAIL',
        recipient: customers['harry.potter'].email,
        templateCode: 'WEEKLY_SUMMARY',
        subject: 'Weekly Portfolio Report',
        body: 'Your weekly portfolio summary will be generated soon.',
        templateVariables: { customerName: 'Harry Potter' },
        metadata: { reportType: 'WEEKLY' },
        status: 'PENDING',
        retryCount: 0,
        maxRetries: 3,
        scheduledAt: new Date(new Date().setDate(new Date().getDate() + 1)),
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

// Insert notifications
db.notifications.insertMany(notifications);
print('Inserted ' + notifications.length + ' notifications');

// =============================================================================
// SEED NOTIFICATION LOGS (in audit database)
// =============================================================================
db = db.getSiblingDB('brokage_audit');

print('Seeding notification_logs collection...');

var notificationLogs = [
    {
        eventId: generateUUID(),
        customerId: customers['peter.parker'].id,
        customerEmail: customers['peter.parker'].email,
        notificationType: 'ORDER_CREATED',
        channel: 'EMAIL',
        recipient: customers['peter.parker'].email,
        subject: 'Order Confirmation - BUY 100 THYAO',
        content: 'Order placed successfully',
        metadata: { orderId: orders.order1 },
        status: 'SENT',
        sentAt: pastDate(1, 0),
        timestamp: pastDate(1, 0),
        createdAt: pastDate(1, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['thanos.titan'].id,
        customerEmail: customers['thanos.titan'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'EMAIL',
        recipient: customers['thanos.titan'].email,
        subject: 'Trade Executed - BUY 500 KCHOL',
        content: 'Order matched and executed',
        metadata: { orderId: orders.order7 },
        status: 'SENT',
        sentAt: pastDate(3, 0),
        timestamp: pastDate(3, 0),
        createdAt: pastDate(3, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['thanos.titan'].id,
        customerEmail: customers['thanos.titan'].email,
        notificationType: 'ORDER_MATCHED',
        channel: 'SMS',
        recipient: '+905551234567',
        subject: null,
        content: 'SMS notification attempted',
        metadata: { orderId: orders.order7 },
        status: 'FAILED',
        errorMessage: 'SMS gateway timeout',
        retryCount: 3,
        timestamp: pastDate(3, 0),
        createdAt: pastDate(3, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['agent.smith'].id,
        customerEmail: customers['agent.smith'].email,
        notificationType: 'ORDER_CANCELED',
        channel: 'EMAIL',
        recipient: customers['agent.smith'].email,
        subject: 'Order Cancelled - BUY 1000 ASELS',
        content: 'Order cancellation confirmed',
        metadata: { orderId: orders.order9 },
        status: 'SENT',
        sentAt: pastDate(5, 0),
        timestamp: pastDate(5, 0),
        createdAt: pastDate(5, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['lex.luthor'].id,
        customerEmail: customers['lex.luthor'].email,
        notificationType: 'DEPOSIT_CONFIRMED',
        channel: 'EMAIL',
        recipient: customers['lex.luthor'].email,
        subject: 'Deposit Confirmed - 1,000,000.00 TRY',
        content: 'Deposit credited to account',
        metadata: { transactionType: 'DEPOSIT', amount: 1000000 },
        status: 'SENT',
        sentAt: pastDate(7, 0),
        timestamp: pastDate(7, 0),
        createdAt: pastDate(7, 0)
    },
    {
        eventId: generateUUID(),
        customerId: customers['barry.allen'].id,
        customerEmail: customers['barry.allen'].email,
        notificationType: 'WELCOME',
        channel: 'EMAIL',
        recipient: customers['barry.allen'].email,
        subject: 'Welcome to Brokage Trading Platform!',
        content: 'Welcome email sent to new customer',
        metadata: { registrationDate: pastDate(30, 0) },
        status: 'SENT',
        sentAt: pastDate(30, 0),
        timestamp: pastDate(30, 0),
        createdAt: pastDate(30, 0)
    }
];

db.notification_logs.insertMany(notificationLogs);
print('Inserted ' + notificationLogs.length + ' notification logs');

// =============================================================================
// SUMMARY
// =============================================================================
print('\n========================================');
print('MongoDB Seed Data Summary:');
print('========================================');
print('Database: brokage_audit');
print('  - audit_logs: ' + auditLogs.length + ' documents');
print('  - notification_logs: ' + notificationLogs.length + ' documents');
print('');
print('Database: brokage_notifications');
print('  - notifications: ' + notifications.length + ' documents');
print('');
print('Characters represented from:');
print('  - Marvel Universe (Peter Parker, Thor, Pepper Potts, Thanos...)');
print('  - Lord of the Rings (Frodo, Samwise, Legolas...)');
print('  - Matrix (Neo, Trinity, Agent Smith...)');
print('  - Harry Potter (Harry, Hermione, Draco...)');
print('  - Justice League (Barry Allen, Arthur Curry, Lex Luthor...)');
print('========================================');
print('MongoDB initialization completed successfully!');
