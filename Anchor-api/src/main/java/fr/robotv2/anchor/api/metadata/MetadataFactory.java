package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.annotation.Index;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MetadataFactory {

    private MetadataFactory() {
        throw new UnsupportedOperationException("This class is a utility class and cannot be instantiated");
    }

    /**
     * Creates EntityMetadata for the specified entity class.
     * <p>
     * This static factory method analyzes the given class and extracts all the
     * necessary metadata information including annotations, field mappings,
     * and index definitions. The class must be properly annotated with
     * {@link Entity} and have exactly one field annotated with both {@link Id}
     * and {@link Column}.
     * </p>
     *
     * @param cls the entity class to analyze, must not be {@code null}
     * @return EntityMetadata containing all information about the entity
     * @throws IllegalArgumentException if the class is not properly annotated
     * @throws IllegalArgumentException if multiple @Id fields are found
     * @throws IllegalArgumentException if no @Id field is found
     * @throws IllegalArgumentException if @Id field is not annotated with @Column
     */
    public static EntityMetadata create(Class<?> cls) {
        if (!cls.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Entity");
        }

        final Entity entity = cls.getAnnotation(Entity.class);
        final Map<String, FieldMetadata> fields = new LinkedHashMap<>();
        final List<IndexMetadata> indexes = new ArrayList<>();

        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(cls, MethodHandles.lookup());
            final Supplier<?> instanceFactory = createInstanceFactory(cls, lookup);

            FieldMetadata idField = null;
            Id id = null;

            for (Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);

                if (field.isAnnotationPresent(Id.class)) {
                    if (idField != null) {
                        throw new IllegalArgumentException("Only one @Id allowed per entity");
                    }

                    if (!field.isAnnotationPresent(Column.class)) {
                        throw new IllegalArgumentException("Id field must be annotated with @Column");
                    }

                    FieldAccessor accessor = FieldAccessor.create(field, lookup);
                    idField = new FieldMetadata(field.getAnnotation(Column.class), field, accessor);
                    id = field.getAnnotation(Id.class);
                    continue;
                }

                if (field.isAnnotationPresent(Column.class)) {
                    Column colAnn = field.getAnnotation(Column.class);
                    FieldAccessor accessor = FieldAccessor.create(field, lookup);
                    fields.put(colAnn.value().toLowerCase(), new FieldMetadata(colAnn, field, accessor));
                }
            }

            if (idField == null) {
                throw new IllegalArgumentException("Entity must have one @Id field");
            }

            processEntityIndexes(cls, indexes);
            processFieldIndexes(cls, indexes);

            return new EntityMetadata(entity, id, idField, fields, indexes, instanceFactory);

        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to create metadata for " + cls.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> createInstanceFactory(Class<T> cls, MethodHandles.Lookup lookup) throws IllegalAccessException {
        try {
            final MethodHandle constructorHandle = lookup.findConstructor(cls, MethodType.methodType(void.class));
            final MethodHandle factorHandle = LambdaMetafactory.metafactory(
                    lookup,
                    "get",
                    MethodType.methodType(Supplier.class),
                    MethodType.methodType(Object.class),
                    constructorHandle,
                    MethodType.methodType(cls)
            ).getTarget();
            return (Supplier<T>) factorHandle.invoke();
        } catch (Throwable throwable) {
            throw new IllegalAccessException("Failed to create instance factory for " + cls.getName() + ": " + throwable.getMessage());
        }
    }

    private static void processEntityIndexes(Class<?> cls, List<IndexMetadata> indexes) {
        if (cls.isAnnotationPresent(Index.class)) {
            Index indexAnnotation = cls.getAnnotation(Index.class);
            String entityName = cls.getAnnotation(Entity.class).value();
            String defaultIndexName = "idx_" + entityName;
            IndexMetadata indexMetadata = IndexMetadata.fromAnnotation(indexAnnotation, defaultIndexName, new ArrayList<>());
            indexes.add(indexMetadata);
        }
    }

    private static void processFieldIndexes(Class<?> cls, List<IndexMetadata> indexes) {
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Index.class) && field.isAnnotationPresent(Column.class)) {
                Index indexAnnotation = field.getAnnotation(Index.class);
                Column columnAnnotation = field.getAnnotation(Column.class);
                String entityName = cls.getAnnotation(Entity.class).value();
                String defaultIndexName = "idx_" + entityName + "_" + columnAnnotation.value();
                IndexMetadata indexMetadata = IndexMetadata.fromAnnotation(indexAnnotation, defaultIndexName, List.of(columnAnnotation.value()));
                indexes.add(indexMetadata);
            }
        }
    }
}
