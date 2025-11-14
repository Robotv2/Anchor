package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Default implementation of AsyncRepository that wraps a synchronous Repository.
 * <p>
 * This class provides a reusable wrapper implementation that delegates all operations
 * to an underlying synchronous repository, executing them asynchronously using the
 * provided executor. This design eliminates code duplication and provides a concrete
 * class that can be extended or customized.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <E> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see AsyncRepository
 * @see Repository
 */
public class AsyncRepositoryWrapper<ID, E extends Identifiable<ID>> implements AsyncRepository<ID, E> {

    protected final Repository<ID, E> repository;
    protected final Executor executor;

    /**
     * Creates a new AsyncRepositoryWrapper.
     *
     * @param repository the synchronous repository to wrap, must not be {@code null}
     * @param executor the executor to use for async operations, must not be {@code null}
     * @throws IllegalArgumentException if repository or executor is {@code null}
     */
    public AsyncRepositoryWrapper(Repository<ID, E> repository, Executor executor) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }
        this.repository = repository;
        this.executor = executor;
    }

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

    /**
     * Gets the underlying synchronous repository.
     *
     * @return the wrapped repository
     */
    public Repository<ID, E> getRepository() {
        return repository;
    }

    /**
     * Gets the executor used for async operations.
     *
     * @return the executor
     */
    public Executor getExecutor() {
        return executor;
    }
}
