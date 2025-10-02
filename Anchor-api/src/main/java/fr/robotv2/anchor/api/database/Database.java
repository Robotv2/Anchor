package fr.robotv2.anchor.api.database;

import fr.robotv2.anchor.api.repository.AsyncRepository;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;

/**
 * Represents a database connection and provides access to repositories for entity operations.
 * <p>
 * The Database interface is the main entry point for interacting with the data store.
 * It manages the database connection lifecycle and provides access to both synchronous
 * and asynchronous repositories for performing CRUD operations on entities.
 * </p>
 *
 * @since 1.0
 * @see Repository
 * @see AsyncRepository
 */
public interface Database {

    /**
     * Establishes a connection to the database.
     * <p>
     * This method should be called before performing any database operations.
     * It initializes the database connection, creates necessary tables if they
     * don't exist, and prepares the database for use. Implementations should
     * handle connection failures gracefully by throwing appropriate exceptions.
     * </p>
     *
     * @throws RuntimeException if the connection cannot be established
     */
    void connect();

    /**
     * Closes the database connection and releases associated resources.
     * <p>
     * This method should be called when the database is no longer needed.
     * It properly closes all connections, releases locks, and cleans up resources.
     * After calling this method, no further database operations should be performed
     * until {@link #connect()} is called again.
     * </p>
     */
    void disconnect();

    /**
     * Checks whether the database is currently connected and ready for operations.
     * <p>
     * This method returns {@code true} if the database connection is active and
     * can accept operations, and {@code false} otherwise. A return value of
     * {@code false} typically indicates that either {@link #connect()} has not
     * been called or the connection has been lost.
     * </p>
     *
     * @return {@code true} if connected, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Returns a repository for performing CRUD operations on the specified entity type.
     * <p>
     * The repository provides methods for saving, finding, updating, and deleting
     * entities of the specified type. The entity class must be properly annotated
     * with {@link fr.robotv2.anchor.api.annotation.Entity} and related annotations.
     * </p>
     *
     * @param <ID> the type of entity identifiers
     * @param <T> the entity type extending {@link Identifiable}
     * @param clazz the entity class to get a repository for
     * @return a repository for the specified entity type
     * @throws IllegalArgumentException if the entity class is not properly annotated
     * @throws IllegalStateException if the database is not connected
     */
    <ID, T extends Identifiable<ID>> Repository<ID, T> getRepository(Class<T> clazz);

    /**
     * Returns an asynchronous repository for performing non-blocking CRUD operations.
     * <p>
     * This is a convenience method that wraps the synchronous repository from
     * {@link #getRepository(Class)} in an {@link AsyncRepository}. The async repository
     * returns {@link java.util.concurrent.CompletableFuture} objects for all operations,
     * allowing for non-blocking database operations.
     * </p>
     *
     * @param <ID> the type of entity identifiers
     * @param <T> the entity type extending {@link Identifiable}
     * @param clazz the entity class to get an async repository for
     * @return an asynchronous repository for the specified entity type
     * @throws IllegalArgumentException if the entity class is not properly annotated
     * @throws IllegalStateException if the database is not connected
     */
    default <ID, T extends Identifiable<ID>> AsyncRepository<ID, T> getAsyncRepository(Class<T> clazz) {
        return AsyncRepository.wrap(getRepository(clazz));
    }
}
