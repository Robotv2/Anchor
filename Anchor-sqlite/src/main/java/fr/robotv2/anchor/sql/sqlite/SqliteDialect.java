package fr.robotv2.anchor.sql.sqlite;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.sql.dialect.ColumnType;
import fr.robotv2.anchor.sql.dialect.SqlCondition;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.dialect.SqlFragment;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SqliteDialect implements SQLDialect {

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
        sb.append(")");
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

        String columns = colNames.stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
        String qTable = quoteIdentifier(tableName);
        String placeholders = colNames.stream().map(c -> "?").collect(Collectors.joining(", "));
        return "INSERT OR REPLACE INTO " + qTable + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    @Override
    public String getAddColumnSql(EntityMetadata metadata, FieldMetadata field) {
        return "ALTER TABLE " + quoteIdentifier(metadata.getEntityName()) + " ADD COLUMN " + getColumnDefinition(field);
    }

    @Override
    public String getCreateIndexSql(EntityMetadata metadata, IndexMetadata index) {
        StringBuilder sb = new StringBuilder("CREATE ");
        if (index.isUnique()) {
            sb.append("UNIQUE ");
        }
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
        return "DROP INDEX IF EXISTS " + quoteIdentifier(index.getName());
    }

    @Override
    public String getSqlType(ColumnType type) {
        if (type == null) {
            return "TEXT";
        }
        return switch (type) {
            case INTEGER, BIGINT, BOOLEAN -> "INTEGER";
            case FLOAT, DOUBLE, DECIMAL -> "REAL";
            case CHAR, VARCHAR, TEXT, DATE, TIMESTAMP, UUID -> "TEXT";
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

        final String symbol = operator.getSymbol(); // "=", "!=", ">", "<", ">=", "<=", "LIKE"
        return new SqlFragment(colSql + " " + symbol + " ?", List.of(value));
    }

    @Override
    public Object toDatabaseValue(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean) return ((Boolean) v) ? 1 : 0;
        if (v instanceof LocalDateTime) return v.toString();
        return SQLDialect.super.toDatabaseValue(v);
    }

    @Override
    public Object fromDatabaseValue(Object v, Class<?> targetType) {
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (v instanceof Number) return ((Number) v).intValue() != 0;
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.equalsIgnoreCase("true")) return true;
                if (s.equalsIgnoreCase("false")) return false;
                try {
                    return Integer.parseInt(s) != 0;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if(targetType == LocalDateTime.class && v instanceof String) {
            return LocalDateTime.parse((String) v);
        }
        return SQLDialect.super.fromDatabaseValue(v, targetType);
    }

    private String getColumnDefinition(FieldMetadata fm, boolean isId) {
        final Column column = fm.getColumn();
        final Field field = fm.getField();
        final ColumnType type = ColumnType.fromJavaClass(field.getType());
        final String sqlType = column.rawType().isEmpty() ? getSqlType(type) : column.rawType();
        String def = quoteIdentifier(column.value()) + " " + sqlType + (isId ? " PRIMARY KEY" : "");
        if (!column.nullable()) {
            def += " NOT NULL";
        }
        return def;
    }

    private String getColumnDefinition(FieldMetadata fm) {
        return getColumnDefinition(fm, false);
    }

    private String getIdColumnDefinition(EntityMetadata metadata) {
        return getColumnDefinition(metadata.getIdField(), true);
    }
}
