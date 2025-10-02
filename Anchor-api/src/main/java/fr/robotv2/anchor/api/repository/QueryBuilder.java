package fr.robotv2.anchor.api.repository;

import java.util.List;

/**
 * Provides a fluent interface for building and executing database queries.
 * <p>
 * QueryBuilder allows for the construction of complex database queries using
 * a method-chaining approach. Queries can include WHERE conditions with
 * logical operators, result limiting, and can return multiple results,
 * a single result, or perform delete operations.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <T> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see Repository
 * @see Operator
 */
public interface QueryBuilder<ID, T extends Identifiable<ID>> {

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
     * @return this QueryBuilder for method chaining
     */
    QueryBuilder<ID, T> where(String column, Operator operator, Object value);

    /**
     * Adds a logical AND operator to the query.
     * <p>
     * This method connects the previous condition with the next condition
     * using logical AND. All conditions connected by AND must be true for
     * a row to match.
     * </p>
     *
     * @return this QueryBuilder for method chaining
     */
    QueryBuilder<ID, T> and();

    /**
     * Adds a logical OR operator to the query.
     * <p>
     * This method connects the previous condition with the next condition
     * using logical OR. At least one of the conditions connected by OR
     * must be true for a row to match.
     * </p>
     *
     * @return this QueryBuilder for method chaining
     */
    QueryBuilder<ID, T> or();

    /**
     * Limits the number of results returned by the query.
     * <p>
     * This method adds a LIMIT clause to the query, restricting the maximum
     * number of rows that can be returned. This is useful for pagination
     * or when only a subset of results is needed.
     * </p>
     *
     * @param count the maximum number of results to return, must be positive
     * @return this QueryBuilder for method chaining
     * @throws IllegalArgumentException if count is not positive
     */
    QueryBuilder<ID, T> limit(int count);

    /**
     * Executes the query and returns all matching entities.
     * <p>
     * This method executes the constructed query and returns a list of all
     * entities that match the specified conditions. If no entities match,
     * an empty list is returned.
     * </p>
     *
     * @return a list of matching entities, may be empty but never {@code null}
     */
    List<T> all();

    /**
     * Executes the query and returns the first matching entity.
     * <p>
     * This method executes the query and returns only the first entity that
     * matches the specified conditions. If no entities match, {@code null}
     * is returned. Use this method when you expect at most one result or
     * only need the first result.
     * </p>
     *
     * @return the first matching entity, or {@code null} if no match found
     */
    T one();

    /**
     * Executes the query as a DELETE operation and returns the number of affected rows.
     * <p>
     * This method deletes all entities that match the specified conditions
     * and returns the count of deleted rows. The query conditions are used
     * to construct the WHERE clause of the DELETE statement.
     * </p>
     *
     * @return the number of entities that were deleted
     */
    int delete();
}
