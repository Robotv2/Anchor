package fr.robotv2.anchor.sql.repository;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.MigrationExecutor;
import fr.robotv2.anchor.sql.database.SQLDatabase;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.repository.migration.ColumnInfo;
import fr.robotv2.anchor.sql.repository.migration.IndexInfo;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface SQLMigrationExecutor extends MigrationExecutor {

    Logger logger = Logger.getLogger(SQLMigrationExecutor.class.getName());

    List<IndexInfo> getTableIndexes() throws SQLException;

    List<ColumnInfo> getTableColumns() throws SQLException;

    default void migrate(SQLDatabase database, EntityMetadata metadata) throws SQLException {
        final SQLDialect dialect = database.getDialect();
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
                final String addColumnSql = dialect.getAddColumnSql(metadata, field);
                database.execute(addColumnSql);
            }

            for (String columnName : columnsToDrop) {
                // TODO: Dropping columns is not yet supported
            }
        }

        // Handle index creation/migration
        migrateIndexes(database, metadata);
    }

    default void migrateIndexes(SQLDatabase database, EntityMetadata metadata) throws SQLException {
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
}
