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
     *
     * @param repository the synchronous QueryableRepository to wrap, must not be {@code null}
     * @param <ID>       the type of entity identifiers
     * @param <E>        the entity type extending {@link Identifiable}
     * @return an AsyncQueryableRepository that wraps the provided repository, never {@code null}
     * @throws IllegalArgumentException if repository is {@code null}
     */
    static <ID, E extends Identifiable<ID>> AsyncQueryableRepository<ID, E> wrap(QueryableRepository<ID, E> repository, Executor executor) {
        return new AsyncQueryableRepositoryWrapper<>(repository, executor);
    }
}
