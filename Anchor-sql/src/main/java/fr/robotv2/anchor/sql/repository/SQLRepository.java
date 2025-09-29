package fr.robotv2.anchor.sql.repository;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.*;
import fr.robotv2.anchor.sql.database.SQLDatabase;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SQLRepository<ID, T extends Identifiable<ID>> implements Repository<ID, T>, Queryable<ID, T> {

    protected final SQLDatabase database;
    protected final Class<T> cls;
    protected final EntityMetadata metadata;
    protected final Logger logger;

    public SQLRepository(SQLDatabase database, Class<T> cls) {
        this.database = database;
        this.cls = cls;
        this.metadata = MetadataProcessor.getMetadata(cls);
        this.logger = Logger.getLogger(this.getClass().getSimpleName());
    }

    public void createTableIfNotExists() {
        try {
            final String createTableSQL = database.getDialect().getCreateTableIfNotExists(metadata);
            database.execute(createTableSQL);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to create table for " + cls.getName(), exception);
        }
    }

    @Override
    public void save(T entity) {
        final String upsert = database.getDialect().getUpsertSql(metadata);
        final Collection<Object> values = metadata.extract(entity).values();

        try {
            database.executeUpdate(upsert, values);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Failed to save entity of type " +  cls.getSimpleName(), exception);
        }
    }

    @Override
    public void saveAll(Collection<T> entities) {
        if(entities.isEmpty()) {
            return;
        }

        final String upsert = database.getDialect().getUpsertSql(metadata);
        final List<Collection<Object>> batchValues = entities.stream().map(metadata::extract).map(Map::values).toList();

        try {
            database.executeBatchUpdate(upsert, batchValues);
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Failed to save entities of type " + cls.getSimpleName(), exception);
        }
    }

    @Override
    public void delete(T entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(ID id) {
        query().where(metadata.getIdField().getColumnName(), Operator.EQUAL, id).delete();
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        if(entities.isEmpty()) {
            return;
        }

        final List<ID> ids = entities.stream().map(Identifiable::getId).toList();
        deleteAllById(ids);
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        if(ids.isEmpty()) {
            return;
        }

        for(ID id : ids) {
            deleteById(id); // TODO: Optimize this batch deletion later or with IN clause
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        if(id == null) {
            return Optional.empty();
        }

        final T value = query().where(metadata.getIdField().getColumnName(), Operator.EQUAL, id).one();
        return Optional.ofNullable(value);
    }

    @Override
    public List<T> findAll() {
        return query().all();
    }

    @Override
    public QueryBuilder<ID, T> query() {
        return new SQLQueryBuilder<>(database, cls);
    }
}
