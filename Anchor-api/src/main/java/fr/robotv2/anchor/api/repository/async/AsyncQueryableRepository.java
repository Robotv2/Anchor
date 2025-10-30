package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryableRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface AsyncQueryableRepository<ID, T extends Identifiable<ID>> extends AsyncRepository<ID, T> {

    /**
     * Creates a new AsyncQueryBuilder for constructing asynchronous database queries.
     * <p>
     * This method returns a fresh AsyncQueryBuilder instance that can be used to
     * construct and execute queries against the database in a non-blocking manner.
     * Each call to this method returns a new, independent AsyncQueryBuilder that
     * maintains its own query state.
     * </p>
     *
     * @return a new AsyncQueryBuilder instance, never {@code null}
     */
    AsyncQueryBuilder<ID, T> query();

    /**
     * Wraps a synchronous QueryableRepository into an asynchronous AsyncQueryableRepository.
     * <p>
     * This method takes an existing QueryableRepository and returns an AsyncQueryableRepository
     * that delegates calls to the underlying repository, executing them asynchronously using
     * CompletableFuture. This is useful for integrating synchronous repositories into
     * asynchronous workflows without blocking the calling thread.
     * </p>
     * <p>
     * <strong>Thread Safety:</strong> The returned AsyncQueryableRepository is thread-safe and can be
     * safely used from multiple threads. All operations are executed asynchronously on the
     * provided executor.
     * </p>
     *
     * @param repository the synchronous QueryableRepository to wrap, must not be {@code null}
     * @param executor the executor to use for asynchronous operations, must not be {@code null}
     * @param <ID>       the type of entity identifiers
     * @param <E>        the entity type extending {@link Identifiable}
     * @return an AsyncQueryableRepository that wraps the provided repository, never {@code null}
     * @throws IllegalArgumentException if repository or executor is {@code null}
     */
    static <ID, E extends Identifiable<ID>> AsyncQueryableRepository<ID, E> wrap(QueryableRepository<ID, E> repository, Executor executor) {
        return new AsyncQueryableRepository<>() {

            @Override
            public AsyncQueryBuilder<ID, E> query() {
                return AsyncQueryBuilder.wrap(repository.query(), executor);
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
        };
    }
}
