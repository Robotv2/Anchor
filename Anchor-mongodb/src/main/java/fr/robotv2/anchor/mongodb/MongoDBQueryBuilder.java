package fr.robotv2.anchor.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.QueryBuilder;
import fr.robotv2.anchor.api.util.EntityMetadataUtil;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MongoDB query builder implementation.
 * <p>
 * This class implements the QueryBuilder interface for MongoDB,
 * providing a fluent API for constructing MongoDB queries.
 * </p>
 *
 * @param <ID> the type of entity identifiers
 * @param <T> the entity type extending {@link Identifiable}
 */
public class MongoDBQueryBuilder<ID, T extends Identifiable<ID>> implements QueryBuilder<ID, T> {

    private final MongoDBRepository<ID, T> repository;
    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final MongoCollection<Document> collection;

    private final List<Bson> conditions = new ArrayList<>();
    private String nextConnector = "AND";
    private Integer limit;

    public MongoDBQueryBuilder(MongoDBRepository<ID, T> repository, Class<T> cls, MongoCollection<Document> collection) {
        this.repository = repository;
        this.cls = cls;
        this.metadata = MetadataProcessor.getMetadata(cls);
        this.collection = collection;
    }

    @Override
    public QueryBuilder<ID, T> where(String column, Operator operator, Object value) {
        String resolved = EntityMetadataUtil.resolveName(metadata, column);
        Bson filter = buildFilter(resolved, operator, value);
        
        if (conditions.isEmpty()) {
            conditions.add(filter);
        } else {
            if ("OR".equals(nextConnector)) {
                // For OR, we need to combine with previous condition
                Bson previous = conditions.remove(conditions.size() - 1);
                conditions.add(Filters.or(previous, filter));
            } else {
                // For AND, just add to the list
                conditions.add(filter);
            }
        }
        
        // Reset connector to default AND
        nextConnector = "AND";
        return this;
    }

    @Override
    public QueryBuilder<ID, T> and() {
        nextConnector = "AND";
        return this;
    }

    @Override
    public QueryBuilder<ID, T> or() {
        nextConnector = "OR";
        return this;
    }

    @Override
    public QueryBuilder<ID, T> limit(int count) {
        this.limit = (count > 0) ? count : null;
        return this;
    }

    @Override
    public List<T> all() {
        Bson filter = buildCombinedFilter();
        List<T> results = new ArrayList<>();
        
        var iterable = (limit != null) 
            ? collection.find(filter).limit(limit)
            : collection.find(filter);
            
        for (Document document : iterable) {
            results.add(repository.documentToEntity(document));
        }
        
        return results;
    }

    @Override
    public T one() {
        Bson filter = buildCombinedFilter();
        Document document = collection.find(filter).limit(1).first();
        
        if (document == null) {
            return null;
        }
        
        return repository.documentToEntity(document);
    }

    @Override
    public int delete() {
        Bson filter = buildCombinedFilter();
        long deletedCount = collection.deleteMany(filter).getDeletedCount();
        return (int) deletedCount;
    }

    /**
     * Builds a MongoDB filter for the given operator and value.
     *
     * @param column the column name
     * @param operator the comparison operator
     * @param value the value to compare against
     * @return the MongoDB filter
     */
    private Bson buildFilter(String column, Operator operator, Object value) {
        // Convert value to MongoDB-compatible type
        Object mongoValue = repository.toMongoValue(value);
        
        return switch (operator) {
            case EQUAL -> (mongoValue == null) ? Filters.eq(column, null) : Filters.eq(column, mongoValue);
            case NOT_EQUAL -> (mongoValue == null) ? Filters.ne(column, null) : Filters.ne(column, mongoValue);
            case GREATER_THAN -> Filters.gt(column, mongoValue);
            case LESS_THAN -> Filters.lt(column, mongoValue);
            case GREATER_THAN_OR_EQUAL -> Filters.gte(column, mongoValue);
            case LESS_THAN_OR_EQUAL -> Filters.lte(column, mongoValue);
            case IN -> {
                if (mongoValue instanceof Collection) {
                    yield Filters.in(column, (Collection<?>) mongoValue);
                }
                yield Filters.eq(column, mongoValue);
            }
        };
    }

    /**
     * Combines all conditions into a single MongoDB filter.
     *
     * @return the combined filter
     */
    private Bson buildCombinedFilter() {
        if (conditions.isEmpty()) {
            return Filters.empty();
        }
        
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        
        // All conditions are already combined with OR where needed,
        // so we just need to AND the remaining ones
        return Filters.and(conditions);
    }
}
