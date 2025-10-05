package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.util.BlobSerializationUtility;
import fr.robotv2.anchor.api.annotation.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Supplier;

/**
 * Metadata and structural information about an entity class.
 * <p>
 * EntityMetadata encapsulates all the information needed to map an entity class
 * to database operations, including table name, column mappings, indexes,
 * and field metadata. This class is typically created by the framework
 * during initialization and used internally by repositories and dialects.
 * </p>
 *
 * @since 1.0
 * @see FieldMetadata
 * @see IndexMetadata
 * @see Entity
 * @see Column
 * @see Id
 * @see Index
 */
public class EntityMetadata {

    private final Entity entity;
    private final Id id;
    private final FieldMetadata idField;
    private final Map<String, FieldMetadata> fields;
    private final List<IndexMetadata> indexes;
    private final Supplier<?> instanceSupplier;

    EntityMetadata(Entity entity, Id id, FieldMetadata idField, Map<String, FieldMetadata> fields, List<IndexMetadata> indexes, Supplier<?> instanceSupplier) {
        this.entity = entity;
        this.id = id;
        this.idField = idField;
        this.fields = fields;
        this.indexes = indexes;
        this.instanceSupplier = instanceSupplier;
    }

    /**
     * Returns the {@link Id} annotation from the entity's primary key field.
     *
     * @return the Id annotation, never {@code null}
     */
    @NotNull
    public Id getId() {
        return id;
    }

    /**
     * Returns metadata for the entity's primary key field.
     *
     * @return the field metadata for the ID field, never {@code null}
     */
    @NotNull
    public FieldMetadata getIdField() {
        return idField;
    }

    /**
     * Returns metadata for a specific field by its column name.
     * <p>
     * The lookup is case-insensitive. This method only returns metadata
     * for non-ID fields. To get the ID field metadata, use {@link #getIdField()}.
     * </p>
     *
     * @param name the column name to search for, must not be {@code null}
     * @return the field metadata if found, {@code null} otherwise
     */
    @Nullable
    public FieldMetadata getField(@NotNull String name) {
        return fields.get(name.toLowerCase());
    }

    /**
     * Returns the {@link Entity} annotation from the entity class.
     *
     * @return the Entity annotation, never {@code null}
     */
    @NotNull
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns the table name for this entity.
     * <p>
     * This is the value specified in the {@link Entity} annotation.
     * </p>
     *
     * @return the table name, never {@code null}
     */
    @NotNull
    public String getEntityName() {
        return entity.value();
    }

    /**
     * Returns an unmodifiable view of all non-ID column fields.
     * <p>
     * The returned map contains column names as keys (case-insensitive)
     * and {@link FieldMetadata} objects as values. The map does not
     * include the ID field.
     * </p>
     *
     * @return an unmodifiable map of field metadata, never {@code null}
     */
    @NotNull
    @UnmodifiableView
    public Map<String, FieldMetadata> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Returns an unmodifiable view of all fields including the ID field.
     * <p>
     * The returned map contains column names as keys (case-insensitive)
     * and {@link FieldMetadata} objects as values. This includes the ID field
     * and all other column fields.
     * </p>
     *
     * @return an unmodifiable map of all field metadata, never {@code null}
     */
    @NotNull
    @UnmodifiableView
    public Map<String, FieldMetadata> getAllFields() {
        Map<String, FieldMetadata> allFields = new LinkedHashMap<>();
        allFields.put(idField.getColumnName().toLowerCase(), idField);
        allFields.putAll(fields);
        return Collections.unmodifiableMap(allFields);
    }

    /**
     * Returns an unmodifiable view of all column names for this entity.
     * <p>
     * This includes the ID column name and all other column names.
     * The names are returned in lowercase.
     * </p>
     *
     * @return an unmodifiable set of column names, never {@code null}
     */
    @NotNull
    @UnmodifiableView
    public Set<String> getAllColumnNames() {
        return Collections.unmodifiableSet(getAllFields().keySet());
    }

    /**
     * Returns an unmodifiable view of all index definitions for this entity.
     * <p>
     * The list includes indexes defined at both the field level and class level.
     * Each index definition contains information about the columns to index
     * and whether the index should be unique.
     * </p>
     *
     * @return an unmodifiable list of index metadata, never {@code null}
     */
    @NotNull
    @UnmodifiableView
    public List<IndexMetadata> getIndexes() {
        return Collections.unmodifiableList(indexes);
    }

    /**
     * Extracts column values from an entity instance into a map.
     * <p>
     * This method reads all field values from the given entity instance
     * and returns a map with column names as keys (case-insensitive) and
     * the corresponding field values as values. The extraction uses
     * safe access methods that handle access exceptions gracefully.
     * </p>
     *
     * @param value the entity instance to extract values from, may be {@code null}
     * @return an unmodifiable map of column names to field values, may be empty if value is {@code null}
     */
    @NotNull
    @UnmodifiableView
    public Map<String, Object> extract(Object value) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (FieldMetadata fm : getAllFields().values()) {
            Object fieldValue = fm.safeGet(value);
            if (fm.isBlob()) {
                fieldValue = BlobSerializationUtility.serialize(fieldValue);
            }
            values.put(fm.getColumnName().toLowerCase(), fieldValue);
        }
        return values;
    }

    /**
     * Creates a new instance of the entity class.
     * <p>
     * Uses an efficient instance factory created with LambdaMetafactory
     * for optimal performance.
     * </p>
     *
     * @param <T> the entity type
     * @return a new instance of the entity
     * @throws RuntimeException if instance creation fails
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T newInstance() {
        try {
            return (T) instanceSupplier.get();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create new entity instance", exception);
        }
    }
}
