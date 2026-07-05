package fr.robotv2.anchor.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a database column that maps to a table column.
 * <p>
 * This annotation is required for all fields that should be persisted in the database
 * except for the field annotated with {@link Id}, which must also be annotated with @Column.
 * The field name in the entity class does not need to match the column name in the database.
 * </p>
 *
 * @since 1.0
 * @see Entity
 * @see Id
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /**
     * The name of the database column that this field maps to.
     * <p>
     * This value will be used as the column name when creating SQL statements
     * for database operations. The name should follow the naming conventions
     * of the target database system.
     * </p>
     *
     * @return the column name for this field
     */
    String value();

    /**
     * Whether this column can contain NULL values in the database.
     * <p>
     * If set to {@code false}, the column will be created with a NOT NULL constraint.
     * Note that the Id field is always non-nullable regardless of this setting.
     * </p>
     *
     * @return {@code true} if the column can be null, {@code false} otherwise
     */
    boolean nullable() default true;

    /**
     * Raw SQL type definition for this column.
     * <p>
     * When specified, this value will be used directly in the CREATE TABLE statement
     * instead of the automatically determined type. This is useful for:
     * <ul>
     *   <li>Specifying custom column types (e.g., TEXT, BLOB)</li>
     *   <li>Adding column constraints (e.g., DEFAULT, UNIQUE)</li>
     *   <li>Using database-specific types</li>
     * </ul>
     *
     * <p><strong>Example:</strong> {@code "VARCHAR(255) UNIQUE"}, {@code "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"}</p>
     *
     * @return the raw SQL type definition, or empty string to use automatic type detection
     */
    String rawType() default "";

    /**
     * Indicates if this column should be treated as a BLOB (Binary Large Object).
     * <p>
     * When set to {@code true}, the field will be serialized to a byte array
     * for storage in the database and deserialized back into the original object
     * when retrieved. This is useful for storing complex objects that do not
     * map directly to standard SQL types.
     * </p>
     * <p>
     * Note that using BLOBs may have performance implications and can complicate
     * querying. Use this option judiciously for fields that truly require it.
     * </p>
     *
     * @return {@code true} if the column is a BLOB, {@code false} otherwise
     */
    boolean blob() default false;
}
