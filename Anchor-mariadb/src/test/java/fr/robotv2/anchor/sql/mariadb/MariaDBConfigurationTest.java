package fr.robotv2.anchor.sql.mariadb;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MariaDBConfigurationTest {

    @Test
    void usesMariaDbDriverForMariaDbUrl() {
        MariaDBConfiguration credentials = new MariaDBConfiguration(
                "database.example.com",
                3307,
                "anchor",
                "user",
                "password"
        );

        HikariConfig config = MariaDBDatabase.createDataSourceConfig(credentials);

        assertEquals("jdbc:mariadb://database.example.com:3307/anchor", config.getJdbcUrl());
        assertEquals("org.mariadb.jdbc.Driver", config.getDriverClassName());
    }
}
