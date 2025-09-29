package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Column;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class FieldMetadata {

    private final Column column;

    private final Field field;

    public FieldMetadata(Column column, Field field) {
        this.column = column;
        this.field = field;
    }

    @NotNull
    public Column getColumn() {
        return column;
    }

    @NotNull
    public String getColumnName() {
        return column.value();
    }

    public boolean isNullable() {
        return column.nullable();
    }

    @Nullable
    public String getRawType() {
        return column.rawType().isEmpty() ? null : column.rawType();
    }

    @NotNull
    public Field getField() {
        return field;
    }

    @ApiStatus.Internal
    public void safeSet(@NotNull Object instance, @Nullable Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to set value for field: " + field.getName(), exception);
        }
    }

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
