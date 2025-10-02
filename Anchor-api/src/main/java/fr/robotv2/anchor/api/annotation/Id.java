package fr.robotv2.anchor.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the primary key identifier for an entity.
 * <p>
 * Every entity must have exactly one field annotated with @Id, and this field
 * must also be annotated with {@link Column}. The Id field serves as the unique
 * identifier for entities and is used for:
 * <ul>
 *   <li>Primary key constraints in database tables</li>
 *   <li>Lookup operations (findById, deleteById)</li>
 *   <li>Entity identity and equality comparisons</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 * @see Entity
 * @see Column
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
}
