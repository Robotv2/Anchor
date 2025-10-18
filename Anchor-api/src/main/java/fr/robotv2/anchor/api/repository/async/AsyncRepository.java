package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.api.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Provides asynchronous CRUD operations for entities using {@link CompletableFuture}.
 * <p>
 * The AsyncRepository interface mirrors the synchronous {@link Repository} interface
 * but returns CompletableFuture objects for all operations, enabling non-blocking
 * database operations. This is particularly useful in applications where
 * database operations should not block the calling thread, such as web servers
 * or UI applications.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <E> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see Repository
 * @see Identifiable
 * @see CompletableFuture
 */
public interface AsyncRepository<ID, E extends Identifiable<ID>> {

    /**
     * Asynchronously saves an entity to the database.
     * <p>
     * This method performs an "upsert" operation similar to {@link Repository#save(Identifiable)}
     * but returns immediately with a CompletableFuture that completes when the
     * operation finishes. The entity will be inserted if it doesn't exist
     * or updated if it does exist, based on its ID.
     * </p>
     *
     * @param entity the entity to save, must not be {@code null}
     * @return a CompletableFuture that completes when the save operation finishes
     * @throws IllegalArgumentException if entity is {@code null}
     */
    CompletableFuture<Void> save(E entity);

    /**
     * Asynchronously saves multiple entities to the database in a batch operation.
     * <p>
     * This method is more efficient than calling {@link #save(Identifiable)} multiple
     * times as it can use database batch operations. The returned CompletableFuture
     * completes when all entities have been saved.
     * </p>
     *
     * @param entities the collection of entities to save, must not be {@code null}
     * @return a CompletableFuture that completes when the batch save operation finishes
     * @throws IllegalArgumentException if entities is {@code null} or contains {@code null} values
     */
    CompletableFuture<Void> saveAll(Collection<E> entities);

    /**
     * Asynchronously deletes an entity from the database.
     * <p>
     * The entity will be deleted based on its ID. The returned CompletableFuture
     * completes when the delete operation finishes.
     * </p>
     *
     * @param entity the entity to delete, must not be {@code null}
     * @return a CompletableFuture that completes when the delete operation finishes
     * @throws IllegalArgumentException if entity is {@code null}
     */
    CompletableFuture<Void> delete(E entity);

    /**
     * Asynchronously deletes an entity from the database by its ID.
     * <p>
     * This method removes the entity with the specified ID from the database.
     * The returned CompletableFuture completes when the operation finishes.
     * If no entity with the given ID exists, the operation still completes successfully.
     * </p>
     *
     * @param id the ID of the entity to delete, must not be {@code null}
     * @return a CompletableFuture that completes when the delete operation finishes
     * @throws IllegalArgumentException if id is {@code null}
     */
    CompletableFuture<Void> deleteById(ID id);

    /**
     * Asynchronously deletes multiple entities from the database in a batch operation.
     * <p>
     * This method is more efficient than calling {@link #delete(Identifiable)} multiple
     * times as it can use database batch operations. The returned CompletableFuture
     * completes when all entities have been deleted.
     * </p>
     *
     * @param entities the collection of entities to delete, must not be {@code null}
     * @return a CompletableFuture that completes when the batch delete operation finishes
     * @throws IllegalArgumentException if entities is {@code null} or contains {@code null} values
     */
    CompletableFuture<Void> deleteAll(Collection<E> entities);

    /**
     * Asynchronously deletes multiple entities from the database by their IDs in a batch operation.
     * <p>
     * This method removes all entities with the specified IDs from the database.
     * The returned CompletableFuture completes when the operation finishes.
     * </p>
     *
     * @param ids the collection of IDs to delete, must not be {@code null}
     * @return a CompletableFuture that completes when the batch delete operation finishes
     * @throws IllegalArgumentException if ids is {@code null} or contains {@code null} values
     */
    CompletableFuture<Void> deleteAllById(Collection<ID> ids);

    /**
     * Asynchronously finds an entity by its ID.
     * <p>
     * Returns a CompletableFuture that completes with an {@link Optional}
     * containing the entity if found, or an empty Optional if no entity
     * with the given ID exists in the database.
     * </p>
     *
     * @param id the ID of the entity to find, must not be {@code null}
     * @return a CompletableFuture that completes with an Optional containing the entity if found
     * @throws IllegalArgumentException if id is {@code null}
     */
    CompletableFuture<Optional<E>> findById(ID id);

    /**
     * Asynchronously retrieves all entities of this type from the database.
     * <p>
     * Returns a CompletableFuture that completes with a list containing all entities
     * in the corresponding database table. Be careful when using this method
     * with large datasets as it may consume significant memory and processing resources.
     * </p>
     *
     * @return a CompletableFuture that completes with a list of all entities, may be empty
     */
    CompletableFuture<List<E>> findAll();

    /**
     * Creates an asynchronous repository that wraps a synchronous repository.
     * <p>
     * This static factory method provides a convenient way to obtain an AsyncRepository
     * implementation that executes operations on the default ForkJoinPool. Each operation
     * from the synchronous repository is wrapped in a CompletableFuture.
     * </p>
     *
     * @param <ID> the type of entity identifiers
     * @param <E> the entity type extending {@link Identifiable}
     * @param repository the synchronous repository to wrap, must not be {@code null}
     * @return an AsyncRepository that delegates to the given synchronous repository
     * @throws IllegalArgumentException if repository is {@code null}
     */
    static <ID, E extends Identifiable<ID>> AsyncRepository<ID, E> wrap(Repository<ID, E> repository, Executor executor) {
        if(repository instanceof QueryableRepository<ID,E> queryable) {
            return AsyncQueryableRepository.wrap(queryable, executor);
        }

        return new AsyncRepository<>() {

            @Override
            public CompletableFuture<Void> save(E entity) {
                return CompletableFuture.runAsync(() -> repository.save(entity), executor);
            }

            @Override
            public CompletableFuture<Void> saveAll(Collection<E> entities) {
                return CompletableFuture.runAsync(() -> repository.saveAll(entities), executor);
            }

            @Override
            public CompletableFuture<Void> delete(E entity) {
                return CompletableFuture.runAsync(() -> repository.delete(entity), executor);
            }

            @Override
            public CompletableFuture<Void> deleteById(ID id) {
                return CompletableFuture.runAsync(() -> repository.deleteById(id), executor);
            }

            @Override
            public CompletableFuture<Void> deleteAll(Collection<E> entities) {
                return CompletableFuture.runAsync(() -> repository.deleteAll(entities), executor);
            }

            @Override
            public CompletableFuture<Void> deleteAllById(Collection<ID> ids) {
                return CompletableFuture.runAsync(() -> repository.deleteAllById(ids), executor);
            }

            @Override
            public CompletableFuture<Optional<E>> findById(ID id) {
                return CompletableFuture.supplyAsync(() -> repository.findById(id), executor);
            }

            @Override
            public CompletableFuture<List<E>> findAll() {
                return CompletableFuture.supplyAsync(repository::findAll, executor);
            }
        };
    }
}
