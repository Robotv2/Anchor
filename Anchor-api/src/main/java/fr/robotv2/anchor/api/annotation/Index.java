package fr.robotv2.anchor.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the creation of a database index for improved query performance.
 * <p>
 * This annotation can be applied to:
 * <ul>
 *   <li>A field (creates an index on that specific column)</li>
 *   <li>An entity class (creates a composite index on multiple columns)</li>
 * </ul>
 *
 * @since 1.0
 * @see Entity
 * @see Column
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Index {

    /**
     * The name of the index to be created.
     * <p>
     * If not specified, a default name will be generated:
     * <ul>
     *   <li>For field-level indexes: {@code "idx_<entity>_<column>"}</li>
     *   <li>For class-level indexes: {@code "idx_<entity>"}</li>
     * </ul>
     * The name should be unique within the database schema.
     *
     * @return the name of the index, or empty string for auto-generated name
     */
    String name() default "";

    /**
     * The columns to include in a composite index.
     * <p>
     * This property is only used when the annotation is applied at the class level.
     * Each value must correspond to the {@code value()} property of a @Column annotation
     * in the same entity class. The order of columns in the array determines the
     * column order in the composite index.
     * </p>
     *
     * @return the array of column names for the composite index
     */
    String[] columns() default {};

    /**
     * Whether this index should enforce uniqueness.
     * <p>
     * If set to {@code true}, the database will enforce that no two rows have
     * the same values for the indexed column(s). This is useful for:
     * <ul>
     *   <li>Preventing duplicate email addresses</li>
     *   <li>Ensuring unique usernames</li>
     *   <li>Creating natural key constraints</li>
     * </ul>
     *
     * @return {@code true} if the index should be unique, {@code false} otherwise
     */
    boolean unique() default false;
}