package fr.robotv2.anchor.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.sql.database.SQLDatabase;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.mapper.RowMapper;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SqliteDatabase implements SQLDatabase {

    private final Map<Class<?>, SqliteRepository<?, ?>> repositories = new ConcurrentHashMap<>();
    private final HikariDataSource source;
    private final SQLDialect dialect = new SqliteDialect();

    public SqliteDatabase(File file) {
        final HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        config.addDataSourceProperty("url", "jdbc:sqlite:" + file.getPath());
        config.addDataSourceProperty("enforceForeignKeys", true); // For data integrity
        config.addDataSourceProperty("journalMode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL"); // A safe and fast-syncing mode
        config.setMaximumPoolSize(1); // prevent locking errors
        config.setPoolName("Anchor-SQLite");
        config.setConnectionTimeout(5_000);
        config.setLeakDetectionThreshold(10_000);
        this.source = new HikariDataSource(config);
    }

    public SqliteDatabase(HikariConfig config, File file) {
        config.setJdbcUrl("jdbc:sqlite:" + file.getPath());
        config.setDriverClassName("org.sqlite.JDBC");
        this.source = new HikariDataSource(config);
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
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> SqliteRepository<ID, T> getRepository(Class<T> cls) {
        return (SqliteRepository<ID, T>) repositories.computeIfAbsent(cls, c -> new SqliteRepository<>(this, cls));
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
        String sql = dialect.getCreateIndexSql(metadata, index);
        return execute(sql);
    }

    @Override
    public boolean dropIndex(EntityMetadata metadata, IndexMetadata index) throws SQLException {
        String sql = dialect.getDropIndexSql(metadata, index);
        return execute(sql);
    }
}
