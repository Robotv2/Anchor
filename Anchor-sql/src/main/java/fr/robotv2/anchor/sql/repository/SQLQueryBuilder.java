package fr.robotv2.anchor.sql.repository;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.QueryBuilder;
import fr.robotv2.anchor.api.util.EntityMetadataUtil;
import fr.robotv2.anchor.sql.database.SQLDatabase;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.dialect.SqlCondition;
import fr.robotv2.anchor.sql.dialect.SqlFragment;
import fr.robotv2.anchor.sql.mapper.EntityRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLQueryBuilder<ID, T extends Identifiable<ID>> implements QueryBuilder<ID, T> {

    private static final Logger logger = LoggerFactory.getLogger(SQLQueryBuilder.class);

    private final SQLDatabase database;
    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final SQLDialect dialect;

    private final List<SqlCondition> conditions = new ArrayList<>();
    private String nextConnector = "AND";
    private Integer limit;

    public SQLQueryBuilder(SQLDatabase database, Class<T> cls) {
        this.database = database;
        this.cls = cls;
        this.metadata = MetadataProcessor.getMetadata(cls);
        this.dialect = database.getDialect();
    }

    @Override
    public QueryBuilder<ID, T> where(String column, Operator operator, Object value) {
        String resolved = EntityMetadataUtil.resolveName(metadata, column);
        SqlFragment frag = dialect.buildPredicate(resolved, operator, value);
        String connector = conditions.isEmpty() ? null : nextConnector;
        conditions.add(new SqlCondition(connector, frag));

        // reset connector to default AND
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
        try {
            return select(this.limit);
        } catch (SQLException exception) {
            logger.error("Failed to execute SELECT all", exception);
            throw new RuntimeException("SELECT failed", exception);
        }
    }

    @Override
    public T one() {
        try {
            final List<T> results = select(1);
            return results.isEmpty() ? null : results.getFirst();
        } catch (SQLException exception) {
            logger.error("Failed to execute SELECT one", exception);
            throw new RuntimeException("SELECT failed", exception);
        }
    }

    @Override
    public int delete() {
        final List<Object> params = new ArrayList<>();
        final String sql = dialect.getDeleteSql(metadata, conditions, params);

        try {
            logger.debug("Executing DELETE: {} | params={}", sql, params);
            return database.executeUpdate(sql, params);
        } catch (SQLException exception) {
            logger.error("Failed to execute DELETE", exception);
            throw new RuntimeException("DELETE failed", exception);
        }
    }

    private List<T> select(Integer limitOverride) throws SQLException {
        final List<Object> params = new ArrayList<>();
        final String sql = dialect.getSelectSql(metadata, conditions, params, limitOverride);
        logger.debug("Executing SELECT: {} | params={}", sql, params);
        final EntityRowMapper<T> mapper = new EntityRowMapper<>(cls, metadata, dialect);
        return database.query(sql, params, mapper);
    }
}
