package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryableRepository;

import java.util.concurrent.Executor;

/**
 * Default implementation of AsyncQueryableRepository that wraps a synchronous QueryableRepository.
 * <p>
 * This class extends AsyncRepositoryWrapper to provide query building capabilities in addition
 * to basic CRUD operations. This design eliminates code duplication and provides a concrete
 * class that can be extended or customized.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <E> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see AsyncQueryableRepository
 * @see QueryableRepository
 */
public class AsyncQueryableRepositoryWrapper<ID, E extends Identifiable<ID>> 
        extends AsyncRepositoryWrapper<ID, E> 
        implements AsyncQueryableRepository<ID, E> {

    private final QueryableRepository<ID, E> queryableRepository;

    /**
     * Creates a new AsyncQueryableRepositoryWrapper.
     *
     * @param repository the synchronous queryable repository to wrap, must not be {@code null}
     * @param executor the executor to use for async operations, must not be {@code null}
     * @throws IllegalArgumentException if repository or executor is {@code null}
     */
    public AsyncQueryableRepositoryWrapper(QueryableRepository<ID, E> repository, Executor executor) {
        super(repository, executor);
        this.queryableRepository = repository;
    }

    @Override
    public AsyncQueryBuilder<ID, E> query() {
        return AsyncQueryBuilder.wrap(queryableRepository.query(), executor);
    }

    /**
     * Gets the underlying synchronous queryable repository.
     *
     * @return the wrapped queryable repository
     */
    public QueryableRepository<ID, E> getQueryableRepository() {
        return queryableRepository;
    }
}
