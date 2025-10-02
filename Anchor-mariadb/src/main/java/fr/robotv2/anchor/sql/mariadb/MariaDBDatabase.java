package fr.robotv2.anchor.sql.mariadb;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.sql.database.HikariDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MariaDBDatabase extends HikariDatabase {

    // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
    public static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
    public static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);
    public static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    public static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    public static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);

    private final Map<Class<?>, MariaDBRepository<?, ?>> repositories = new ConcurrentHashMap<>();

    public MariaDBDatabase(HikariDataSource source) {
        super(source, new MariaDBDialect());
    }

    public MariaDBDatabase(MariaDBConfiguration configuration) {
        this(defaultConfig(configuration));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> MariaDBRepository<ID, T> getRepository(Class<T> cls) {
        return (MariaDBRepository<ID, T>) repositories.computeIfAbsent(cls, c -> new MariaDBRepository<>(this, cls));
    }

    private static HikariDataSource defaultConfig(MariaDBConfiguration credentials) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + credentials.host() + ":" + credentials.port() + "/" + credentials.database());
        config.setUsername(credentials.username());
        config.setPassword(credentials.password());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("Anchor-Mariadb");
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("alwaysSendSetIsolation", "false");
        config.addDataSourceProperty("cacheCallableStmts", "true");
        // Set the driver level TCP socket timeout
        // See: https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
        config.addDataSourceProperty("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
        return new HikariDataSource(config);
    }
}
