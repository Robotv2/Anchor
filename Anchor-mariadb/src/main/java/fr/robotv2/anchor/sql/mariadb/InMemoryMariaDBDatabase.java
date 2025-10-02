package fr.robotv2.anchor.sql.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class InMemoryMariaDBDatabase extends MariaDBDatabase {

    public InMemoryMariaDBDatabase() {
        super(inMemory());
    }

    private static HikariDataSource inMemory() {
        final HikariConfig config = new HikariConfig();

        // H2 in-memory URL with MySQL mode
        config.setJdbcUrl("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setUsername("sa"); // Default H2 user
        config.setPassword("");   // Default H2 password
        config.setDriverClassName("org.h2.Driver");

        config.setPoolName("Anchor-H2-Test");
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        return new HikariDataSource(config);
    }
}
