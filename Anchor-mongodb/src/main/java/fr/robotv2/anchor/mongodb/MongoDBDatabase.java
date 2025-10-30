package fr.robotv2.anchor.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.database.SupportType;
import fr.robotv2.anchor.api.repository.Identifiable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB database implementation.
 * <p>
 * This class provides MongoDB database connectivity and repository management
 * for the Anchor ORM. It supports asynchronous operations and migrations.
 * </p>
 */
public class MongoDBDatabase implements Database {

    private static final Set<SupportType> SUPPORTED_TYPES = EnumSet.of(
            SupportType.WRAPPED_ASYNC,
            SupportType.MIGRATION
    );

    private final Map<Class<?>, MongoDBRepository<?, ?>> repositories = new ConcurrentHashMap<>();
    private final MongoDBConfiguration configuration;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean connected;

    /**
     * Creates a new MongoDB database instance with the provided configuration.
     *
     * @param configuration the MongoDB connection configuration
     */
    public MongoDBDatabase(MongoDBConfiguration configuration) {
        this.configuration = configuration;
        this.connected = false;
    }

    @Override
    public void connect() {
        if (connected) {
            return;
        }

        try {
            // Build connection string
            StringBuilder connectionStringBuilder = new StringBuilder("mongodb://");
            
            if (configuration.username() != null && !configuration.username().isEmpty()) {
                connectionStringBuilder.append(configuration.username());
                if (configuration.password() != null && !configuration.password().isEmpty()) {
                    connectionStringBuilder.append(":").append(configuration.password());
                }
                connectionStringBuilder.append("@");
            }
            
            connectionStringBuilder.append(configuration.host())
                    .append(":")
                    .append(configuration.port())
                    .append("/")
                    .append(configuration.database());

            ConnectionString connectionString = new ConnectionString(connectionStringBuilder.toString());
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(configuration.database());
            connected = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to MongoDB", e);
        }
    }

    @Override
    public void disconnect() {
        if (!connected) {
            return;
        }

        try {
            if (mongoClient != null) {
                mongoClient.close();
            }
            connected = false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to disconnect from MongoDB", e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean supports(SupportType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> MongoDBRepository<ID, T> getRepository(Class<T> cls) {
        if (!isConnected()) {
            throw new IllegalStateException("Database is not connected");
        }
        return (MongoDBRepository<ID, T>) repositories.computeIfAbsent(cls, c -> new MongoDBRepository<>(this, cls));
    }

    /**
     * Gets the MongoDB database instance.
     *
     * @return the MongoDB database
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Gets the MongoDB configuration.
     *
     * @return the configuration
     */
    public MongoDBConfiguration getConfiguration() {
        return configuration;
    }
}
