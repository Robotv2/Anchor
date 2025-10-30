package fr.robotv2.anchor.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryBuilder;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.api.repository.Repository;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB repository implementation.
 * <p>
 * This class implements the QueryableRepository interface for MongoDB,
 * providing CRUD operations and query building for entities stored in MongoDB collections.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <T> the entity type extending {@link Identifiable}
 */
public class MongoDBRepository<ID, T extends Identifiable<ID>> implements QueryableRepository<ID, T> {

    private final MongoDBDatabase database;
    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final MongoCollection<Document> collection;
    private final String idColumnName;

    public MongoDBRepository(MongoDBDatabase database, Class<T> cls) {
        this.database = database;
        this.cls = cls;
        this.metadata = MetadataProcessor.getMetadata(cls);
        this.collection = database.getDatabase().getCollection(metadata.getEntityName());
        this.idColumnName = metadata.getIdField().getColumnName();
    }

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        
        Document document = entityToDocument(entity);
        Bson filter = Filters.eq(idColumnName, convertId(entity.getId()));
        
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        collection.replaceOne(filter, document, options);
    }

    @Override
    public void saveAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        if (entities.isEmpty()) {
            return;
        }
        
        // Use bulk write operations for better performance
        List<com.mongodb.client.model.WriteModel<Document>> writes = new ArrayList<>();
        for (T entity : entities) {
            Document document = entityToDocument(entity);
            Bson filter = Filters.eq(idColumnName, convertId(entity.getId()));
            com.mongodb.client.model.ReplaceOneModel<Document> replaceModel = 
                new com.mongodb.client.model.ReplaceOneModel<>(filter, document, new ReplaceOptions().upsert(true));
            writes.add(replaceModel);
        }
        
        if (!writes.isEmpty()) {
            collection.bulkWrite(writes);
        }
    }

    @Override
    public void delete(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        Bson filter = Filters.eq(idColumnName, convertId(id));
        collection.deleteOne(filter);
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        if (entities.isEmpty()) {
            return;
        }
        
        // Extract IDs and use deleteAllById for better performance
        List<ID> ids = new ArrayList<>();
        for (T entity : entities) {
            ids.add(entity.getId());
        }
        deleteAllById(ids);
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        Objects.requireNonNull(ids, "IDs collection cannot be null");
        if (ids.isEmpty()) {
            return;
        }
        
        List<Object> convertedIds = new ArrayList<>();
        for (ID id : ids) {
            convertedIds.add(convertId(id));
        }
        
        Bson filter = Filters.in(idColumnName, convertedIds);
        collection.deleteMany(filter);
    }

    @Override
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        
        Bson filter = Filters.eq(idColumnName, convertId(id));
        Document document = collection.find(filter).first();
        
        if (document == null) {
            return Optional.empty();
        }
        
        return Optional.of(documentToEntity(document));
    }

    @Override
    public List<T> findAll() {
        List<T> results = new ArrayList<>();
        for (Document document : collection.find()) {
            results.add(documentToEntity(document));
        }
        return results;
    }

    @Override
    public QueryBuilder<ID, T> query() {
        return new MongoDBQueryBuilder<>(this, cls, collection);
    }

    /**
     * Converts an entity to a MongoDB Document.
     *
     * @param entity the entity to convert
     * @return the MongoDB document
     */
    private Document entityToDocument(T entity) {
        Document document = new Document();
        
        // Add ID field
        document.append(idColumnName, convertId(entity.getId()));
        
        // Add all other fields
        for (FieldMetadata fieldMetadata : metadata.getFields().values()) {
            try {
                Field field = fieldMetadata.getField();
                field.setAccessible(true);
                Object value = field.get(entity);
                
                // Convert value to MongoDB-compatible type
                Object convertedValue = toMongoValue(value);
                document.append(fieldMetadata.getColumnName(), convertedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + fieldMetadata.getField().getName(), e);
            }
        }
        
        return document;
    }

    /**
     * Converts a MongoDB Document to an entity.
     * Package-private to allow access from MongoDBQueryBuilder.
     *
     * @param document the MongoDB document
     * @return the entity
     */
    T documentToEntity(Document document) {
        try {
            T entity = cls.getDeclaredConstructor().newInstance();
            
            // Set ID field
            Field idField = metadata.getIdField().getField();
            idField.setAccessible(true);
            Object idValue = document.get(idColumnName);
            idField.set(entity, fromMongoValue(idValue, idField.getType()));
            
            // Set all other fields
            for (FieldMetadata fieldMetadata : metadata.getFields().values()) {
                Field field = fieldMetadata.getField();
                field.setAccessible(true);
                Object value = document.get(fieldMetadata.getColumnName());
                field.set(entity, fromMongoValue(value, field.getType()));
            }
            
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create entity from document", e);
        }
    }

    /**
     * Converts ID to MongoDB-compatible value.
     *
     * @param id the ID to convert
     * @return the MongoDB-compatible ID
     */
    private Object convertId(ID id) {
        if (id instanceof UUID) {
            return id.toString();
        }
        return id;
    }

    /**
     * Converts a value to MongoDB-compatible type.
     * Package-private to allow access from MongoDBQueryBuilder.
     *
     * @param value the value to convert
     * @return the MongoDB-compatible value
     */
    Object toMongoValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof UUID) {
            return value.toString();
        }
        
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
        }
        
        return value;
    }

    /**
     * Converts a MongoDB value to Java type.
     *
     * @param value the MongoDB value
     * @param targetType the target Java type
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    private Object fromMongoValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType == UUID.class && value instanceof String) {
            return UUID.fromString((String) value);
        }
        
        if (targetType == LocalDateTime.class && value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
        }
        
        // Handle numeric conversions
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            } else if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            }
        }
        
        return value;
    }
}
