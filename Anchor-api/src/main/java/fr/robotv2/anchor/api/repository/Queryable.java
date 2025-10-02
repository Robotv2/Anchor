package fr.robotv2.anchor.api.repository;

/**
 * Interface for repositories that support querying with conditions.
 * <p>
 * Queryable extends the basic repository contract to provide support for
 * building and executing complex database queries. Repositories implementing
 * this interface can create {@link QueryBuilder} instances for constructing
 * queries with WHERE conditions, logical operators, and result limiting.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * Queryable<Long, User> userRepo = database.getRepository(User.class);
 *
 * // Find active users older than 18
 * List<User> adults = userRepo.query()
 *     .where("age", Operator.GREATER_THAN, 18)
 *     .and()
 *     .where("status", Operator.EQUAL, "ACTIVE")
 *     .all();
 *
 * // Delete inactive users
 * int deleted = userRepo.query()
 *     .where("last_login", Operator.LESS_THAN, cutoffDate)
 *     .delete();
 * }</pre>
 *
 * @param <ID> the type of entity identifiers
 * @param <T> the entity type extending {@link Identifiable}
 * @since 1.0
 * @see QueryBuilder
 * @see Repository
 * @see Operator
 */
public interface Queryable<ID, T extends Identifiable<ID>> {

    /**
     * Creates a new QueryBuilder for constructing database queries.
     * <p>
     * This method returns a fresh QueryBuilder instance that can be used to
     * construct and execute queries against the database. Each call to this
     * method returns a new, independent QueryBuilder that maintains its own
     * query state.
     * </p>
     *
     * @return a new QueryBuilder instance, never {@code null}
     */
    QueryBuilder<ID, T> query();
}
