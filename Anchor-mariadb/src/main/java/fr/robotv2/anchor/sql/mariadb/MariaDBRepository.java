package fr.robotv2.anchor.sql.mariadb;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.sql.repository.SQLMigrationExecutor;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.repository.migration.ColumnInfo;
import fr.robotv2.anchor.sql.repository.migration.IndexInfo;

import java.sql.SQLException;
import java.util.List;

public class MariaDBRepository<ID, T extends Identifiable<ID>> extends SQLRepository<ID, T> implements SQLMigrationExecutor {

    private final MariaDBDatabase database;
    private final EntityMetadata metadata;

    public MariaDBRepository(MariaDBDatabase database, Class<T> cls) {
        super(database, cls);
        this.database = database;
        this.metadata = MetadataProcessor.getMetadata(cls);
    }

    @Override
    public List<IndexInfo> getTableIndexes() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String sql = "SELECT DISTINCT INDEX_NAME AS name " +
                "FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE LOWER(TABLE_SCHEMA) = LOWER(SCHEMA()) " +
                "AND LOWER(TABLE_NAME) = LOWER(?) " +
                "ORDER BY INDEX_NAME";

        try {
            return database.query(sql, List.of(tableName), rs -> new IndexInfo(rs.getString("name").toLowerCase()));
        } catch (SQLException e) {
            // Testing env for H2 compatibility
            final String h2Sql = "SELECT DISTINCT INDEX_NAME AS name " +
                    "FROM INFORMATION_SCHEMA.INDEXES " +
                    "WHERE LOWER(TABLE_SCHEMA) = LOWER(SCHEMA()) " +
                    "AND LOWER(TABLE_NAME) = LOWER(?) " +
                    "ORDER BY INDEX_NAME";

            return database.query(h2Sql, List.of(tableName), rs -> new IndexInfo(rs.getString("name").toLowerCase()));
        }
    }

    @Override
    public List<ColumnInfo> getTableColumns() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String sql = "SELECT COLUMN_NAME AS name " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE LOWER(TABLE_SCHEMA) = LOWER(SCHEMA()) " +
                "AND LOWER(TABLE_NAME) = LOWER(?) " +
                "ORDER BY ORDINAL_POSITION";
        return database.query(sql, List.of(tableName), rs -> new ColumnInfo(rs.getString("name").toLowerCase()));
    }

    @Override
    public void migrate() throws SQLException {
        SQLMigrationExecutor.super.migrate(database, metadata);
    }
}
