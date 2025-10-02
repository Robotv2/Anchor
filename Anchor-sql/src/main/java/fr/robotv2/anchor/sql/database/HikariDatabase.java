package fr.robotv2.anchor.sql.database;

import com.zaxxer.hikari.HikariDataSource;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.mapper.RowMapper;
import org.jetbrains.annotations.ApiStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class HikariDatabase implements SQLDatabase {

    protected final HikariDataSource source;
    protected final SQLDialect dialect;

    @ApiStatus.Internal
    protected HikariDatabase(DataSource source, SQLDialect dialect) {
        if(!(source instanceof HikariDataSource)) {
            throw new IllegalArgumentException("DataSource must be an instance of HikariDataSource");
        }
        this.source = (HikariDataSource) source;
        this.dialect = dialect;
    }

    @Override
    public void connect() {}

    @Override
    public void disconnect() {
        if (this.source != null) {
            this.source.close();
        }
    }

    @Override
    public boolean isConnected() {
        return source != null && source.isRunning();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.source.getConnection();
    }

    @Override
    public SQLDialect getDialect() {
        return dialect;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.execute();
        }
    }

    @Override
    public int executeUpdate(String sql, Collection<Object> parameters) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object param : parameters) {
                statement.setObject(index++, dialect.toDatabaseValue(param));
            }
            return statement.executeUpdate();
        }
    }

    @Override
    public int executeBatchUpdate(String sql, Collection<Collection<Object>> parameters) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Collection<Object> params : parameters) {
                int index = 1;
                for (Object param : params) {
                    statement.setObject(index++, dialect.toDatabaseValue(param));
                }
                statement.addBatch();
            }
            final int[] results = statement.executeBatch();
            return Arrays.stream(results).sum();
        }
    }

    @Override
    public <R> List<R> query(String sql, Collection<Object> parameters, RowMapper<R> mapper) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object param : parameters) {
                statement.setObject(index++, dialect.toDatabaseValue(param));
            }
            List<R> results = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        }
    }

    @Override
    public <R> List<R> queryRaw(String sql, RowMapper<R> mapper) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            List<R> results = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
            return results;
        }
    }

    @Override
    public boolean createIndex(EntityMetadata metadata, IndexMetadata index) throws SQLException {
        return execute(dialect.getCreateIndexSql(metadata, index));
    }

    @Override
    public boolean dropIndex(EntityMetadata metadata, IndexMetadata index) throws SQLException {
        return execute(dialect.getDropIndexSql(metadata, index));
    }
}
