package fr.robotv2.anchor.sql.repository;

import java.sql.SQLException;

public interface MigrationExecutor {

    void migrate() throws SQLException;
}
