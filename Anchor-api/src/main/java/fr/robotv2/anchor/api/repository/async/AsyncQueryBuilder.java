package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.QueryBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides a fluent interface for building and executing asynchronous database queries.
 * <p>
 * AsyncQueryBuilder allows for the construction of complex database queries using
 * a method-chaining approach. Queries can include WHERE conditions with
 * logical operators, result limiting, and can return multiple results,
 * a single result, or perform delete operations, all in a non-blocking manner.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <T> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see AsyncRepository
 * @see Operator
 */
public interface AsyncQueryBuilder<ID, T extends Identifiable<ID>> {

    /**
     * Adds a WHERE condition to the query.
     * <p>
     * This method adds a comparison condition to the WHERE clause. Multiple
     * conditions can be chained together using {@link #and()} or {@link #or()}
     * methods to create complex queries.
     * </p>
     *
     * @param column the column name to compare, must not be {@code null}
     * @param operator the comparison operator, must not be {@code null}
     * @param value the value to compare against, may be {@code null}
     * @return this AsyncQueryBuilder for method chaining
     */
    AsyncQueryBuilder<ID, T> where(String column, Operator operator, Object value);

    /**
     * Adds a logical AND operator to the query.
     * <p>
     * This method connects the previous condition with the next condition
     * using logical AND. All conditions connected by AND must be true for
     * a row to match.
     * </p>
     *
     * @return this AsyncQueryBuilder for method chaining
     */
    AsyncQueryBuilder<ID, T> and();

    /**
     * Adds a logical OR operator to the query.
     * <p>
     * This method connects the previous condition with the next condition
     * using logical OR. At least one of the conditions connected by OR
     * must be true for a row to match.
     * </p>
     *
     * @return this AsyncQueryBuilder for method chaining
     */
    AsyncQueryBuilder<ID, T> or();

    /**
     * Limits the number of results returned by the query.
     * <p>
     * This method restricts the maximum number of rows returned by the query.
     * If not called, all matching rows will be returned.
     * </p>
     *
     * @param count the maximum number of results to return, must be greater than zero
     * @return this AsyncQueryBuilder for method chaining
     * @throws IllegalArgumentException if count is less than or equal to zero
     */
    AsyncQueryBuilder<ID, T> limit(int count);

    /**
     * Executes the query and retrieves all matching results asynchronously.
     * <p>
     * This method executes the constructed query and returns a CompletableFuture
     * that completes with a list of all matching entities. If no entities match,
     * the list will be empty.
     * </p>
     *
     * @return a CompletableFuture that completes with a list of matching entities
     */
    CompletableFuture<List<T>> all();

    /**
     * Executes the query and retrieves a single matching result asynchronously.
     * <p>
     * This method executes the constructed query and returns a CompletableFuture
     * that completes with the single matching entity. If no entities match,
     * the future completes with {@code null}. If multiple entities match,
     * an exception is thrown.
     * </p>
     *
     * @return a CompletableFuture that completes with the single matching entity, or {@code null} if none found
     * @throws IllegalStateException if more than one entity matches the query
     */
    CompletableFuture<T> one();

    /**
     * Executes a delete operation for all entities matching the query asynchronously.
     * <p>
     * This method deletes all entities that match the constructed query conditions.
     * It returns a CompletableFuture that completes with the number of rows deleted.
     * </p>
     *
     * @return a CompletableFuture that completes with the number of rows deleted
     */
    CompletableFuture<Integer> delete();

    /**
     * Wraps a synchronous QueryBuilder into an asynchronous AsyncQueryBuilder.
     * <p>
     * This static method takes an existing QueryBuilder instance and returns
     * an AsyncQueryBuilder that executes the same queries asynchronously using
     * CompletableFutures. This allows for non-blocking database operations while
     * reusing existing query logic.
     * </p>
     *
     * @param builder the synchronous QueryBuilder to wrap, must not be {@code null}
     * @param <ID> the type of entity identifiers
     * @param <E> the entity type extending {@link Identifiable}
     * @return an AsyncQueryBuilder that wraps the provided QueryBuilder
     * @throws IllegalArgumentException if builder is {@code null}
     */
    static <ID, E extends Identifiable<ID>> AsyncQueryBuilder<ID, E> wrap(QueryBuilder<ID, E> builder) {
        return new AsyncQueryBuilder<>() {

            @Override
            public AsyncQueryBuilder<ID, E> where(String column, Operator operator, Object value) {
                builder.where(column, operator, value);
                return this;
            }

            @Override
            public AsyncQueryBuilder<ID, E> and() {
                builder.and();
                return this;
            }

            @Override
            public AsyncQueryBuilder<ID, E> or() {
                builder.or();
                return this;
            }

            @Override
            public AsyncQueryBuilder<ID, E> limit(int count) {
                builder.limit(count);
                return this;
            }

            @Override
            public CompletableFuture<List<E>> all() {
                return CompletableFuture.supplyAsync(builder::all);
            }

            @Override
            public CompletableFuture<E> one() {
                return CompletableFuture.supplyAsync(builder::one);
            }

            @Override
            public CompletableFuture<Integer> delete() {
                return CompletableFuture.supplyAsync(builder::delete);
            }
        };
    }
}
