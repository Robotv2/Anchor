package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EntityMetadata {

    private final Entity entity;

    private final Id id;

    private final FieldMetadata idField;

    private final Map<String, FieldMetadata> fields;

    private EntityMetadata(Entity entity, Id id, FieldMetadata idField, Map<String, FieldMetadata> fields) {
        this.entity = entity;
        this.id = id;
        this.idField = idField;
        this.fields = fields;
    }

    @NotNull
    public Id getId() {
        return id;
    }

    @NotNull
    public FieldMetadata getIdField() {
        return idField;
    }

    @Nullable
    public FieldMetadata getField(@NotNull String name) {
        return fields.get(name.toLowerCase());
    }

    @NotNull
    public Entity getEntity() {
        return entity;
    }

    @NotNull
    public String getEntityName() {
        return entity.value();
    }

    @NotNull
    @UnmodifiableView
    public Map<String, FieldMetadata> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    @NotNull
    @UnmodifiableView
    public Map<String, FieldMetadata> getAllFields() {
        Map<String, FieldMetadata> allFields = new LinkedHashMap<>();
        allFields.put(idField.getColumnName().toLowerCase(), idField);
        allFields.putAll(fields);
        return Collections.unmodifiableMap(allFields);
    }

    @NotNull
    @UnmodifiableView
    public Set<String> getAllColumnNames() {
        return Collections.unmodifiableSet(getAllFields().keySet());
    }

    @NotNull
    @UnmodifiableView
    public Map<String, Object> extract(Object value) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (FieldMetadata fm : getAllFields().values()) {
            values.put(fm.getColumnName().toLowerCase(), fm.safeGet(value));
        }
        return values;
    }

    public static EntityMetadata create(Class<?> cls) {
        if (!cls.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Entity");
        }

        final Entity entity = cls.getAnnotation(Entity.class);
        final Map<String, FieldMetadata> fields = new LinkedHashMap<>();

        FieldMetadata idField = null;
        Id id = null;

        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Id.class)) {
                if (idField != null) {
                    throw new IllegalArgumentException("Only one @Id allowed per entity");
                }
                if(!field.isAnnotationPresent(Column.class)) {
                    throw new IllegalArgumentException("Id field must be annotated with @Column");
                }

                idField = new FieldMetadata(field.getAnnotation(Column.class), field);
                id = field.getAnnotation(Id.class);
                continue;
            }

            if (field.isAnnotationPresent(Column.class)) {
                Column colAnn = field.getAnnotation(Column.class);
                fields.put(colAnn.value().toLowerCase(), new FieldMetadata(colAnn, field));
            }
        }

        if (idField == null) {
            throw new IllegalArgumentException("Entity must have one @Id field");
        }

        return new EntityMetadata(entity, id, idField, fields);
    }
}
