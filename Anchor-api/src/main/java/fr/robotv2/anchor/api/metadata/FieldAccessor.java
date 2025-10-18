package fr.robotv2.anchor.api.metadata;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Efficient accessor for reading and writing field values using MethodHandles.
 * <p>
 * FieldAccessor provides high-performance field access operations by leveraging
 * MethodHandles instead of reflection. Instances are created during metadata
 * initialization and cached for optimal performance.
 * </p>
 *
 * @since 1.0
 * @see FieldMetadata
 */
@ApiStatus.Internal
public final class FieldAccessor {

    private final MethodHandle getter;
    private final MethodHandle setter;
    private final Class<?> fieldType;

    private FieldAccessor(MethodHandle getter, MethodHandle setter, Class<?> fieldType) {
        this.getter = getter;
        this.setter = setter;
        this.fieldType = fieldType;
    }

    /**
     * Gets the value of the field from the given instance.
     *
     * @param instance the object instance to read from, must not be {@code null}
     * @return the field value, may be {@code null}
     * @throws RuntimeException if the field cannot be accessed
     */
    @Nullable
    public Object get(@NotNull Object instance) {
        try {
            return getter.invoke(instance);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get field value", e);
        }
    }

    /**
     * Sets the value of the field on the given instance.
     *
     * @param instance the object instance to write to, must not be {@code null}
     * @param value the value to set, may be {@code null}
     * @throws RuntimeException if the field cannot be accessed
     */
    public void set(@NotNull Object instance, @Nullable Object value) {
        try {
            setter.invoke(instance, value);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }

    /**
     * Returns the Java type of this field.
     *
     * @return the field type, never {@code null}
     */
    @NotNull
    public Class<?> getFieldType() {
        return fieldType;
    }

    /**
     * Returns the underlying getter MethodHandle.
     * <p>
     * Exposed for advanced use cases that need direct MethodHandle access.
     * </p>
     *
     * @return the getter MethodHandle, never {@code null}
     */
    @NotNull
    public MethodHandle getGetter() {
        return getter;
    }

    /**
     * Returns the underlying setter MethodHandle.
     * <p>
     * Exposed for advanced use cases that need direct MethodHandle access.
     * </p>
     *
     * @return the setter MethodHandle, never {@code null}
     */
    @NotNull
    public MethodHandle getSetter() {
        return setter;
    }

    /**
     * Creates a FieldAccessor for the specified field.
     *
     * @param field the field to create an accessor for, must not be {@code null}
     * @param lookup the MethodHandles.Lookup with access to the field's class
     * @return a new FieldAccessor instance
     * @throws IllegalStateException if the accessor cannot be created
     */
    @NotNull
    static FieldAccessor create(@NotNull Field field, @NotNull MethodHandles.Lookup lookup) {
        try {
            field.setAccessible(true);
            MethodHandle getter = lookup.unreflectGetter(field);
            MethodHandle setter = lookup.unreflectSetter(field);
            return new FieldAccessor(getter, setter, field.getType());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to create FieldAccessor for field: " + field.getName(), exception);
        }
    }
}
