package fr.robotv2.anchor.sql.mariadb.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.robotv2.anchor.sql.mariadb.MariaDBDatabase;

final class H2MariaDBCompatibilityDatabase extends MariaDBDatabase {

    H2MariaDBCompatibilityDatabase() {
        super(inMemoryDataSource());
    }

    private static HikariDataSource inMemoryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setPoolName("Anchor-H2-Compatibility");
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);
        return new HikariDataSource(config);
    }
}
