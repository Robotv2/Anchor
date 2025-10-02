package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Column;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Metadata for a single entity field that maps to a database column.
 * <p>
 * FieldMetadata encapsulates information about a field in an entity class,
 * including the {@link Column} annotation and the underlying Java Field.
 * This provides safe access to field values and column configuration
 * for database operations and type conversion.
 * </p>
 *
 * @since 1.0
 * @see EntityMetadata
 * @see Column
 */
public class FieldMetadata {

    private final Column column;

    private final Field field;

    /**
     * Creates FieldMetadata for a field with its column annotation.
     *
     * @param column the Column annotation for this field, must not be {@code null}
     * @param field the Java Field reference, must not be {@code null}
     */
    public FieldMetadata(Column column, Field field) {
        this.column = column;
        this.field = field;
    }

    /**
     * Returns the {@link Column} annotation for this field.
     *
     * @return the Column annotation, never {@code null}
     */
    @NotNull
    public Column getColumn() {
        return column;
    }

    /**
     * Returns the database column name for this field.
     * <p>
     * This is the value specified in the {@link Column} annotation.
     * </p>
     *
     * @return the column name, never {@code null}
     */
    @NotNull
    public String getColumnName() {
        return column.value();
    }

    /**
     * Returns whether this column can contain NULL values.
     * <p>
     * This corresponds to the {@code nullable} property of the {@link Column} annotation.
     * Note that ID fields are always non-nullable regardless of this setting.
     * </p>
     *
     * @return {@code true} if the column can be null, {@code false} otherwise
     */
    public boolean isNullable() {
        return column.nullable();
    }

    /**
     * Returns the raw SQL type definition for this column.
     * <p>
     * If the {@link Column} annotation specifies a raw type, that value is returned.
     * Otherwise, {@code null} is returned to indicate that the database dialect
     * should determine the appropriate type based on the field's Java type.
     * </p>
     *
     * @return the raw SQL type, or {@code null} if using automatic type detection
     */
    @Nullable
    public String getRawType() {
        return column.rawType().isEmpty() ? null : column.rawType();
    }

    /**
     * Returns the Java Field reference for this metadata.
     *
     * @return the Field object, never {@code null}
     */
    @NotNull
    public Field getField() {
        return field;
    }

    /**
     * Safely gets the field value from an entity instance.
     * <p>
     * This method uses reflection to get the field value and handles any
     * {@link IllegalAccessException} by wrapping it in a {@link RuntimeException}.
     * The field is made accessible during metadata creation.
     * </p>
     *
     * @param instance the entity instance to get the value from, must not be {@code null}
     * @return the field value, may be {@code null}
     * @throws RuntimeException if the field cannot be accessed
     * @apiNote This method is intended for internal framework use
     */
    @Nullable
    @ApiStatus.Internal
    Object safeGet(@NotNull Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to get value for field: " + field.getName(), exception);
        }
    }
}
