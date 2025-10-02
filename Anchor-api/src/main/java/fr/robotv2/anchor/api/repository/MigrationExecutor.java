package fr.robotv2.anchor.api.repository;

/**
 * Interface for executing database schema migrations.
 * <p>
 * MigrationExecutor provides a contract for performing database schema changes
 * required to update the database structure to match the current entity definitions.
 * This includes creating tables, adding columns, creating indexes, and other
 * structural modifications needed to support the entity mappings.
 * </p>
 *
 * <p><strong>Typical migration tasks:</strong></p>
 * <ul>
 *   <li>Creating new tables for entities</li>
 *   <li>Adding new columns to existing tables</li>
 *   <li>Creating or updating indexes</li>
 *   <li>Modifying column types or constraints</li>
 *   <li>Handling data transformation during schema changes</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * database.migrate(); // Apply all pending migrations
 * }</pre>
 *
 * @since 1.0
 */
public interface MigrationExecutor {

    /**
     * Executes all pending database migrations.
     * <p>
     * This method analyzes the current database schema and applies any necessary
     * changes to bring it in sync with the current entity definitions. The migration
     * process should be idempotent - running it multiple times should not cause
     * issues once the schema is up to date.
     * </p>
     *
     * @throws Exception if the migration fails for any reason (connection issues,
     *                    SQL errors, constraint violations, etc.)
     */
    void migrate() throws Exception;
}
