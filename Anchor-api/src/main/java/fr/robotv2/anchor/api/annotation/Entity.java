package fr.robotv2.anchor.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a database entity that can be persisted and retrieved.
 * <p>
 * An entity represents a table in the database and must have:
 * <ul>
 *   <li>Exactly one field annotated with {@link Id}</li>
 *   <li>One or more fields annotated with {@link Column}</li>
 *   <li>A no-argument constructor (either explicit or default)</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 * @see Column
 * @see Id
 * @see Index
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {

    /**
     * The name of the database table that this entity maps to.
     * <p>
     * This value will be used as the table name when creating SQL statements
     * for database operations. The name should follow the naming conventions
     * of the target database system.
     * </p>
     *
     * @return the table name for this entity
     */
    String value();
}
