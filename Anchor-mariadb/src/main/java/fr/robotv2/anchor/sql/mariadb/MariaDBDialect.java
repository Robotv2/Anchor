package fr.robotv2.anchor.sql.mariadb;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.sql.dialect.ColumnType;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.dialect.SqlCondition;
import fr.robotv2.anchor.sql.dialect.SqlFragment;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MariaDBDialect implements SQLDialect {

    // MariaDB typically formats DATETIME as "yyyy-MM-dd HH:mm:ss[.fraction]"
    private static final DateTimeFormatter MARIADB_DATETIME_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
                    .optionalEnd()
                    .toFormatter();

    @Override
    public String getCreateTableIfNotExists(EntityMetadata metadata) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(quoteIdentifier(metadata.getEntityName())).append(" (");
        List<String> colDefs = new ArrayList<>();

        // ID column
        colDefs.add(getIdColumnDefinition(metadata));
        // Other columns
        for (FieldMetadata fm : metadata.getFields().values()) {
            colDefs.add(getColumnDefinition(fm));
        }

        sb.append(String.join(", ", colDefs));
        // Reasonable defaults for MariaDB; safe on most installations
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        return sb.toString();
    }

    @Override
    public String getDropTableSql(EntityMetadata metadata) {
        return "DROP TABLE IF EXISTS " + quoteIdentifier(metadata.getEntityName());
    }

    @Override
    public String getUpsertSql(EntityMetadata metadata) {
        String tableName = metadata.getEntityName();
        String idColName = metadata.getIdField().getColumnName();

        List<String> colNames = new ArrayList<>();
        colNames.add(idColName);
        for (FieldMetadata fm : metadata.getFields().values()) {
            colNames.add(fm.getColumnName());
        }

        String columns =
                colNames.stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
        String qTable = quoteIdentifier(tableName);
        String placeholders =
                colNames.stream().map(c -> "?").collect(Collectors.joining(", "));

        // ON DUPLICATE KEY UPDATE assignments (avoid updating PK unless necessary)
        List<String> updates = new ArrayList<>();
        for (String col : colNames) {
            if (!col.equals(idColName)) {
                String qCol = quoteIdentifier(col);
                updates.add(qCol + " = VALUES(" + qCol + ")");
            }
        }
        // Edge case: if there are no non-ID columns, perform a no-op update on the PK
        if (updates.isEmpty()) {
            String qId = quoteIdentifier(idColName);
            updates.add(qId + " = " + qId);
        }
        String updateSql = String.join(", ", updates);

        return "INSERT INTO "
                + qTable
                + " ("
                + columns
                + ") VALUES ("
                + placeholders
                + ") ON DUPLICATE KEY UPDATE "
                + updateSql;
    }

    @Override
    public String getAddColumnSql(EntityMetadata metadata, FieldMetadata field) {
        return "ALTER TABLE " + quoteIdentifier(metadata.getEntityName()) + " ADD COLUMN " + getColumnDefinition(field);
    }

    @Override
    public String getCreateIndexSql(EntityMetadata metadata, IndexMetadata index) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (index.isUnique()) sb.append("UNIQUE ");
        sb.append("INDEX ");
        sb.append(quoteIdentifier(index.getName()));
        sb.append(" ON ");
        sb.append(quoteIdentifier(metadata.getEntityName()));
        sb.append(" (");
        String columnList = index.getColumns().stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
        sb.append(columnList);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getDropIndexSql(EntityMetadata metadata, IndexMetadata index) {
        return "DROP INDEX " + quoteIdentifier(index.getName()) + " ON " + quoteIdentifier(metadata.getEntityName());
    }

    @Override
    public String getSqlType(ColumnType type) {
        if (type == null) {
            return "VARCHAR(255)";
        }
        return switch (type) {
            case INTEGER -> "INT";
            case BIGINT -> "BIGINT";
            case BOOLEAN -> "TINYINT(1)";
            case FLOAT -> "FLOAT";
            case DOUBLE -> "DOUBLE";
            case DECIMAL -> "DECIMAL(38,10)";
            case CHAR -> "CHAR(1)";
            case VARCHAR -> "VARCHAR(255)";
            case TEXT -> "TEXT";
            case DATE -> "DATE";
            case TIMESTAMP -> "DATETIME(6)";
            case UUID -> "CHAR(36)";
            case BLOB -> "BLOB";
        };
    }

    @Override
    public String getLimitClause(Integer limit) {
        if (limit == null || limit <= 0) return "";
        return " LIMIT " + limit;
    }

    @Override
    public String getSelectSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params, Integer limit) {
        final String tableName = quoteIdentifier(metadata.getEntityName());
        final List<FieldMetadata> fms = new LinkedList<>(metadata.getAllFields().values());
        final String colListSql = fms.stream().map(FieldMetadata::getColumnName).map(this::quoteIdentifier).collect(Collectors.joining(", "));
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(colListSql).append(" FROM ").append(tableName);
        String where = buildWhereClauseAndCollectParams(conditions, params);
        sql.append(where);
        sql.append(getLimitClause(limit));
        return sql.toString();
    }

    @Override
    public String getDeleteSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params) {
        final String tableName = quoteIdentifier(metadata.getEntityName());
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tableName);
        String where = buildWhereClauseAndCollectParams(conditions, params);
        sql.append(where);
        return sql.toString();
    }

    @Override
    public SqlFragment buildPredicate(String column, Operator operator, Object value) {
        final String colSql = quoteIdentifier(column);
        // Null-safe handling for EQUAL / NOT_EQUAL
        if (value == null) {
            if (operator == Operator.EQUAL) {
                return new SqlFragment(colSql + " IS NULL", List.of());
            }
            if (operator == Operator.NOT_EQUAL) {
                return new SqlFragment(colSql + " IS NOT NULL", List.of());
            }
            throw new IllegalArgumentException("NULL value only supported with EQUAL or NOT_EQUAL");
        }

        final String symbol = operator.getSymbol(); // e.g., =, !=, >, <, >=, <=, LIKE
        return new SqlFragment(colSql + " " + symbol + " ?", List.of(value));
    }

    @Override
    public Object toDatabaseValue(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean) return ((Boolean) v) ? 1 : 0;
        return SQLDialect.super.toDatabaseValue(v);
    }

    @Override
    public Object fromDatabaseValue(Object v, Class<?> targetType) {
        if (targetType == LocalDateTime.class && v instanceof String s) {
            try {
                return LocalDateTime.parse(s, MARIADB_DATETIME_FORMAT);
            } catch (DateTimeParseException ignored) {
                // Fall through to defaults if it isn't the typical MariaDB format
            }
        }
        return SQLDialect.super.fromDatabaseValue(v, targetType);
    }

    private String getColumnDefinition(FieldMetadata fm, boolean isId) {
        final Column column = fm.getColumn();
        final Field field = fm.getField();
        final ColumnType type = ColumnType.fromJavaClass(field.getType());
        final String sqlType = column.rawType().isEmpty() ? getSqlType(type) : column.rawType();
        StringBuilder def = new StringBuilder();
        def.append(quoteIdentifier(column.value())).append(" ").append(sqlType);
        if (isId) def.append(" PRIMARY KEY");
        if (!column.nullable()) def.append(" NOT NULL");
        return def.toString();
    }

    private String getColumnDefinition(FieldMetadata fm) {
        return getColumnDefinition(fm, false);
    }

    private String getIdColumnDefinition(EntityMetadata metadata) {
        return getColumnDefinition(metadata.getIdField(), true);
    }

    @Override
    public String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) return identifier;
        String escaped = identifier.replace("`", "``");
        return "`" + escaped + "`";
    }
}
