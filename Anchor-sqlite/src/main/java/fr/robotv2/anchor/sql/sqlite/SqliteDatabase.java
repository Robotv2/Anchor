package fr.robotv2.anchor.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.sql.database.HikariDatabase;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqliteDatabase extends HikariDatabase {

    private final Map<Class<?>, SqliteRepository<?, ?>> repositories = new ConcurrentHashMap<>();

    public SqliteDatabase(File file) {
        super(defaultConfig(file), new SqliteDialect());
    }

    public SqliteDatabase(HikariConfig config, File file) {
        super(defaultConfig(config, file), new SqliteDialect());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> SqliteRepository<ID, T> getRepository(Class<T> cls) {
        return (SqliteRepository<ID, T>) repositories.computeIfAbsent(cls, c -> new SqliteRepository<>(this, cls));
    }

    private static HikariDataSource defaultConfig(File file) {
        final HikariConfig config = new HikariConfig();
        config.addDataSourceProperty("enforceForeignKeys", true); // For data integrity
        config.addDataSourceProperty("journalMode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL"); // A safe and fast-syncing mode
        config.setMaximumPoolSize(1); // prevent locking errors
        config.setPoolName("Anchor-SQLite");
        config.setConnectionTimeout(5_000);
        config.setLeakDetectionThreshold(10_000);
        return defaultConfig(config, file);
    }

    private static HikariDataSource defaultConfig(HikariConfig config, File file) {
        config.addDataSourceProperty("url", "jdbc:sqlite:" + file.getPath());
        config.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        return new HikariDataSource(config);
    }
}
