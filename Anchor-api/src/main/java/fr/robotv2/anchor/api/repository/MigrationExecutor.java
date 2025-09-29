package fr.robotv2.anchor.api.repository;

import java.sql.SQLException;

public interface MigrationExecutor {

    void migrate() throws SQLException;
}
