package com.brokage.integration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

/**
 * Testcontainers configuration for integration tests.
 *
 * This class provides isolated containers for:
 * - PostgreSQL (with seed data from init-multiple-dbs.sh)
 * - MongoDB (with seed data from init-mongo.js)
 * - Kafka (for event streaming)
 * - Keycloak (with realm-export.json)
 *
 * All containers share a common network for inter-container communication.
 */
public class TestcontainersConfig {

    private static final Logger log = LoggerFactory.getLogger(TestcontainersConfig.class);

    // Shared network for all containers
    public static final Network NETWORK = Network.newNetwork();

    // Container aliases (for inter-container DNS)
    public static final String POSTGRES_ALIAS = "postgres";
    public static final String MONGODB_ALIAS = "mongodb";
    public static final String KAFKA_ALIAS = "kafka";
    public static final String KEYCLOAK_ALIAS = "keycloak";

    // Database credentials (matching init scripts)
    public static final String POSTGRES_DB = "brokage";
    public static final String POSTGRES_USER = "brokage";
    public static final String POSTGRES_PASSWORD = "brokage123";

    public static final String MONGODB_USER = "brokage";
    public static final String MONGODB_PASSWORD = "brokage123";

    // Keycloak credentials
    public static final String KEYCLOAK_ADMIN = "admin";
    public static final String KEYCLOAK_ADMIN_PASSWORD = "admin123";
    public static final String KEYCLOAK_REALM = "brokage";

    // Init script paths (relative to deployment/config)
    private static final String POSTGRES_INIT_SCRIPT = "deployment/config/postgres/init-multiple-dbs.sh";
    private static final String MONGODB_INIT_SCRIPT = "deployment/config/mongodb/init-mongo.js";
    private static final String KEYCLOAK_REALM_EXPORT = "deployment/config/keycloak/realm-export.json";

    // =========================================================================
    // PostgreSQL Container
    // =========================================================================
    @SuppressWarnings("resource")
    public static PostgreSQLContainer<?> createPostgresContainer() {
        log.info("Creating PostgreSQL container with init script...");

        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withNetwork(NETWORK)
                .withNetworkAliases(POSTGRES_ALIAS)
                .withDatabaseName(POSTGRES_DB)
                .withUsername(POSTGRES_USER)
                .withPassword(POSTGRES_PASSWORD)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("init-multiple-dbs.sh"),
                        "/docker-entrypoint-initdb.d/init-multiple-dbs.sh"
                )
                .withStartupTimeout(Duration.ofMinutes(2))
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    }

    // =========================================================================
    // MongoDB Container
    // =========================================================================
    @SuppressWarnings("resource")
    public static MongoDBContainer createMongoContainer() {
        log.info("Creating MongoDB container with init script...");

        return new MongoDBContainer(DockerImageName.parse("mongo:7"))
                .withNetwork(NETWORK)
                .withNetworkAliases(MONGODB_ALIAS)
                .withEnv("MONGO_INITDB_ROOT_USERNAME", MONGODB_USER)
                .withEnv("MONGO_INITDB_ROOT_PASSWORD", MONGODB_PASSWORD)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("init-mongo.js"),
                        "/docker-entrypoint-initdb.d/init-mongo.js"
                )
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    // =========================================================================
    // Kafka Container
    // =========================================================================
    @SuppressWarnings("resource")
    public static KafkaContainer createKafkaContainer() {
        log.info("Creating Kafka container...");

        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                .withNetwork(NETWORK)
                .withNetworkAliases(KAFKA_ALIAS)
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    // =========================================================================
    // Keycloak Container
    // =========================================================================
    @SuppressWarnings("resource")
    public static GenericContainer<?> createKeycloakContainer() {
        log.info("Creating Keycloak container with realm import...");

        return new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:23.0"))
                .withNetwork(NETWORK)
                .withNetworkAliases(KEYCLOAK_ALIAS)
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN)
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD)
                .withEnv("KC_HEALTH_ENABLED", "true")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("realm-export.json"),
                        "/opt/keycloak/data/import/realm-export.json"
                )
                .withCommand("start-dev", "--import-realm")
                .withStartupTimeout(Duration.ofMinutes(3))
                .waitingFor(Wait.forHttp("/health/ready").forPort(8080).withStartupTimeout(Duration.ofMinutes(3)));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Gets the JDBC URL for PostgreSQL container.
     */
    public static String getPostgresJdbcUrl(PostgreSQLContainer<?> postgres) {
        return postgres.getJdbcUrl();
    }

    /**
     * Gets the connection string for MongoDB container.
     */
    public static String getMongoConnectionString(MongoDBContainer mongo) {
        return mongo.getReplicaSetUrl();
    }

    /**
     * Gets the bootstrap servers for Kafka container.
     */
    public static String getKafkaBootstrapServers(KafkaContainer kafka) {
        return kafka.getBootstrapServers();
    }

    /**
     * Gets the Keycloak URL from container.
     */
    public static String getKeycloakUrl(GenericContainer<?> keycloak) {
        return String.format("http://%s:%d", keycloak.getHost(), keycloak.getMappedPort(8080));
    }

    /**
     * Gets the token endpoint URL for Keycloak.
     */
    public static String getKeycloakTokenUrl(GenericContainer<?> keycloak) {
        return getKeycloakUrl(keycloak) + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";
    }
}
