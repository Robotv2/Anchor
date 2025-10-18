package fr.robotv2.anchor.sql.database;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.database.SupportType;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.mapper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Extension of the Database interface for SQL-based databases.
 * <p>
 * SQLDatabase provides additional methods specific to SQL database operations,
 * including direct SQL execution, batch operations, query execution with custom
 * row mappers, and index management. This interface bridges the generic
 * Database contract with SQL-specific capabilities.
 * </p>
 *
 * @since 1.0
 * @see Database
 * @see SQLDialect
 * @see RowMapper
 */
public interface SQLDatabase extends Database {

    /**
     * Returns the underlying JDBC connection.
     * <p>
     * This method provides direct access to the JDBC Connection for advanced
     * use cases that require custom SQL operations or transaction management.
     * Callers should be careful to properly manage the connection and handle
     * any SQLException that may occur.
     * </p>
     *
     * @return the JDBC connection, never {@code null}
     * @throws SQLException if the connection cannot be obtained or the database is not connected
     * @throws IllegalStateException if the database is not connected
     */
    Connection getConnection() throws SQLException;

    /**
     * Returns the SQL dialect for this database.
     * <p>
     * The dialect provides database-specific SQL generation capabilities
     * and type conversion logic. This is used internally by repositories
     * and query builders to generate appropriate SQL statements.
     * </p>
     *
     * @return the SQL dialect, never {@code null}
     */
    SQLDialect getDialect();

    /**
     * Executes a SQL statement without parameters.
     * <p>
     * This method is suitable for DDL statements (CREATE, DROP, ALTER) or
     * DML statements that don't require parameters. For statements with
     * parameters, use {@link #executeUpdate(String, Collection)}.
     * </p>
     *
     * @param sql the SQL statement to execute, must not be {@code null}
     * @return {@code true} if the statement executed successfully, {@code false} otherwise
     * @throws SQLException if the statement execution fails
     * @throws IllegalStateException if the database is not connected
     */
    boolean execute(String sql) throws SQLException;

    /**
     * Executes a parameterized SQL update statement.
     * <p>
     * This method executes an INSERT, UPDATE, or DELETE statement with
     * parameterized values to prevent SQL injection. Parameters are bound
     * in the order they appear in the collection.
     * </p>
     *
     * @param sql the parameterized SQL statement, must not be {@code null}
     * @param parameters the parameter values to bind, may be empty
     * @return the number of rows affected by the update
     * @throws SQLException if the statement execution fails
     * @throws IllegalStateException if the database is not connected
     */
    int executeUpdate(String sql, Collection<Object> parameters) throws SQLException;

    /**
     * Executes a batch update operation with multiple parameter sets.
     * <p>
     * This method is optimized for performance when executing the same
     * SQL statement multiple times with different parameter values. It uses
     * JDBC batch operations to reduce round-trips to the database.
     * </p>
     *
     * @param sql the parameterized SQL statement to execute, must not be {@code null}
     * @param parameters collection of parameter sets, each representing one batch execution
     * @return the total number of rows affected across all batch executions
     * @throws SQLException if the batch execution fails
     * @throws IllegalStateException if the database is not connected
     */
    int executeBatchUpdate(String sql, Collection<Collection<Object>> parameters) throws SQLException;

    /**
     * Executes a parameterized query and maps results using a custom RowMapper.
     * <p>
     * This method provides flexibility for executing custom SQL queries that
     * may not be supported by the standard repository interface. The RowMapper
     * is responsible for converting each ResultSet row into the desired type.
     * </p>
     *
     * @param <R> the result type
     * @param sql the parameterized SQL query, must not be {@code null}
     * @param parameters the parameter values to bind, may be empty
     * @param mapper the RowMapper to convert ResultSet rows to objects, must not be {@code null}
     * @return a list of mapped results, may be empty but never {@code null}
     * @throws SQLException if the query execution fails
     * @throws IllegalStateException if the database is not connected
     */
    <R> List<R> query(String sql, Collection<Object> parameters, RowMapper<R> mapper) throws SQLException;

    /**
     * Executes a SQL query without parameters and maps results using a custom RowMapper.
     * <p>
     * This is a convenience method for queries that don't require parameters.
     * It's equivalent to calling {@link #query(String, Collection, RowMapper)} with
     * an empty parameters collection.
     * </p>
     *
     * @param <R> the result type
     * @param sql the SQL query to execute, must not be {@code null}
     * @param mapper the RowMapper to convert ResultSet rows to objects, must not be {@code null}
     * @return a list of mapped results, may be empty but never {@code null}
     * @throws SQLException if the query execution fails
     * @throws IllegalStateException if the database is not connected
     */
    <R> List<R> queryRaw(String sql, RowMapper<R> mapper) throws SQLException;

    /**
     * Creates a database index for the specified entity and index metadata.
     * <p>
     * This method generates and executes the appropriate CREATE INDEX statement
     * for the target database. The index definition comes from the entity's
     * {@link IndexMetadata}.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param index the index metadata defining the index to create, must not be {@code null}
     * @return {@code true} if the index was created successfully, {@code false} otherwise
     * @throws SQLException if the index creation fails
     * @throws IllegalStateException if the database is not connected
     */
    boolean createIndex(EntityMetadata metadata, IndexMetadata index) throws SQLException;

    /**
     * Drops a database index for the specified entity and index metadata.
     * <p>
     * This method generates and executes the appropriate DROP INDEX statement
     * for the target database. The index definition comes from the entity's
     * {@link IndexMetadata}.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param index the index metadata defining the index to drop, must not be {@code null}
     * @return {@code true} if the index was dropped successfully, {@code false} otherwise
     * @throws SQLException if the index drop fails
     * @throws IllegalStateException if the database is not connected
     */
    boolean dropIndex(EntityMetadata metadata, IndexMetadata index) throws SQLException;

    /**
     * Checks if the database supports a specific feature.
     * <p>
     * This method allows querying the capabilities of the underlying
     * database implementation, such as support for transactions,
     * batch updates, or specific SQL features.
     * </p>
     *
     * @param type the support type to check, must not be {@code null}
     * @return {@code true} if the feature is supported, {@code false} otherwise
     */
    boolean supports(SupportType type);
}
