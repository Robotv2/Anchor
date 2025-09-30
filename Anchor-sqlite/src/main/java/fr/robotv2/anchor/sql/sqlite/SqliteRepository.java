package fr.robotv2.anchor.sql.sqlite;

import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.MigrationExecutor;
import fr.robotv2.anchor.sql.database.SQLDatabase;
import fr.robotv2.anchor.sql.repository.SQLRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SqliteRepository<ID, T extends Identifiable<ID>> extends SQLRepository<ID, T> implements MigrationExecutor {

    public SqliteRepository(SQLDatabase database, Class<T> cls) {
        super(database, cls);
    }

    @Override
    public void migrate() throws SQLException {
        final String tableName = metadata.getEntityName();
        final Set<String> databaseColumns = getTableColumns().stream().map(ColumnInfo::name).collect(Collectors.toSet());
        final Set<String> entityColumns = metadata.getAllColumnNames();

        // Identify columns to add (in entity but not in DB)
        final List<FieldMetadata> columnsToAdd = entityColumns.stream()
                .filter(col -> !databaseColumns.contains(col))
                .map(metadata::getField)
                .filter(Objects::nonNull) // Ensure field metadata exists
                .toList();

        // Identify columns to drop (in DB but not in entity)
        final List<String> columnsToDrop = databaseColumns.stream()
                .filter(col -> !entityColumns.contains(col))
                .toList();

        if (columnsToAdd.isEmpty() && columnsToDrop.isEmpty()) {
            logger.info("No schema changes detected for entity '" + tableName + "'");
        } else {
            for (FieldMetadata field : columnsToAdd) {
                logger.info("Adding column '" + field.getColumnName() + "' to table '" + tableName + "'");
                final String addColumnSql = database.getDialect().getAddColumnSql(metadata, field);
                database.execute(addColumnSql);
            }

            for (String columnName : columnsToDrop) {
                // TODO: Dropping columns in SQLite is not yet supported
            }
        }

        // Handle index creation/migration
        migrateIndexes();
    }

    private void migrateIndexes() throws SQLException {
        final String tableName = metadata.getEntityName();
        final Set<String> existingIndexes = getTableIndexes().stream().map(IndexInfo::name).collect(Collectors.toSet());

        // Create indexes that don't exist
        for (IndexMetadata index : metadata.getIndexes()) {
            if (!existingIndexes.contains(index.getName())) {
                logger.info("Creating index '" + index.getName() + "' on table '" + tableName + "'");
                database.createIndex(metadata, index);
            }
        }
    }

    public List<IndexInfo> getTableIndexes() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String quotedTableName = database.getDialect().quoteIdentifier(tableName);
        final String sql = "PRAGMA index_list(" + quotedTableName + ")";
        return database.queryRaw(sql, (rs) -> new IndexInfo(
                rs.getString("name"),
                rs.getString("unique")
        ));
    }

    public List<ColumnInfo> getTableColumns() throws SQLException {
        final String tableName = metadata.getEntityName();
        final String quotedTableName = database.getDialect().quoteIdentifier(tableName);
        final String sql = "PRAGMA table_info(" + quotedTableName + ")";
        return database.queryRaw(sql, (rs) -> new ColumnInfo(
                rs.getInt("cid"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getInt("notnull") == 1,
                rs.getString("dflt_value"),
                rs.getInt("pk") == 1
        ));
    }

    public record IndexInfo(String name, String unique) { }

    public record ColumnInfo(int cid, String name, String type, boolean notnull, String dflt_value, boolean pk) { }
}
