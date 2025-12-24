// =============================================================================
// MongoDB - Database Initialization
// =============================================================================

// Switch to admin database for authentication
db = db.getSiblingDB('admin');

// Create application databases and users
const databases = [
    { name: 'brokage_notifications', collections: ['notifications', 'templates', 'preferences'] },
    { name: 'brokage_audit', collections: ['audit_logs', 'telemetry', 'compliance'] }
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

// Create indexes for notifications database
db = db.getSiblingDB('brokage_notifications');

db.notifications.createIndex({ "customerId": 1, "createdAt": -1 });
db.notifications.createIndex({ "customerId": 1, "readStatus": 1 });
db.notifications.createIndex({ "channels.email.status": 1 });
db.notifications.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

print('Notifications indexes created');

// Create indexes for audit database
db = db.getSiblingDB('brokage_audit');

db.audit_logs.createIndex({ "timestamp": 1 });
db.audit_logs.createIndex({ "userId": 1, "timestamp": -1 });
db.audit_logs.createIndex({ "entityType": 1, "entityId": 1 });
db.audit_logs.createIndex({ "action": 1, "timestamp": -1 });

db.telemetry.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 604800 }); // 7 days TTL

print('Audit indexes created');

print('MongoDB initialization completed successfully!');
