package fr.robotv2.anchor.sql.sqlite;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.sql.database.SQLDatabase;
import fr.robotv2.anchor.sql.repository.SQLMigrationExecutor;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.repository.migration.ColumnInfo;
import fr.robotv2.anchor.sql.repository.migration.IndexInfo;

import java.sql.SQLException;
import java.util.List;

public class SqliteRepository<ID, T extends Identifiable<ID>> extends SQLRepository<ID, T> implements SQLMigrationExecutor {

    public SqliteRepository(SQLDatabase database, Class<T> cls) {
        super(database, cls);
    }

    @Override
    public List<IndexInfo> getTableIndexes() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String quotedTableName = database.getDialect().quoteIdentifier(tableName);
        final String sql = "PRAGMA index_list(" + quotedTableName + ")";
        return database.queryRaw(sql, (rs) -> new IndexInfo(
                rs.getString("name")
        ));
    }

    @Override
    public List<ColumnInfo> getTableColumns() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String quotedTableName = database.getDialect().quoteIdentifier(tableName);
        final String sql = "PRAGMA table_info(" + quotedTableName + ")";
        return database.queryRaw(sql, (rs) -> new ColumnInfo(
                rs.getString("name")
        ));
    }

    @Override
    public void migrate() throws SQLException {
        SQLMigrationExecutor.super.migrate(database, metadata);
    }
}
