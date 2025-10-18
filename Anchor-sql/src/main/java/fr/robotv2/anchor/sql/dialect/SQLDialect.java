package fr.robotv2.anchor.sql.dialect;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.util.BlobSerializationUtility;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Interface for database-specific SQL generation and type conversion.
 * <p>
 * SQLDialect provides the contract for generating database-specific SQL statements
 * and handling type conversions between Java types and database types. Each supported
 * database system (SQLite, MariaDB, etc.) implements this interface to provide
 * the appropriate SQL syntax and type handling for that database.
 * </p>
 *
 * @since 1.0
 * @see EntityMetadata
 * @see FieldMetadata
 * @see IndexMetadata
 */
public interface SQLDialect {

    /**
     * Generates SQL to create a table if it doesn't already exist.
     * <p>
     * This method should create a CREATE TABLE IF NOT EXISTS statement with all
     * columns defined according to the entity metadata. The statement should include
     * primary key constraints and appropriate data types for the target database.
     * </p>
     *
     * @param metadata the entity metadata containing table and column information, must not be {@code null}
     * @return SQL statement to create the table, never {@code null}
     */
    String getCreateTableIfNotExists(EntityMetadata metadata);

    /**
     * Generates SQL to drop a table.
     * <p>
     * This method should create a DROP TABLE statement that removes the table
     * if it exists. The implementation should use the appropriate syntax
     * for the target database (e.g., DROP TABLE IF EXISTS).
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @return SQL statement to drop the table, never {@code null}
     */
    String getDropTableSql(EntityMetadata metadata);

    /**
     * Generates SQL for an upsert (INSERT OR UPDATE) operation.
     * <p>
     * This method should create an SQL statement that inserts a new row or
     * updates an existing row based on the primary key. The exact syntax
     * varies between databases (e.g., ON DUPLICATE KEY UPDATE for MySQL,
     * INSERT OR REPLACE for SQLite).
     * </p>
     *
     * @param metadata the entity metadata containing table and column information, must not be {@code null}
     * @return SQL statement for upsert operation, never {@code null}
     */
    String getUpsertSql(EntityMetadata metadata);

    /**
     * Generates SQL to add a new column to an existing table.
     * <p>
     * This method should create an ALTER TABLE statement that adds a new column
     * with the appropriate data type and constraints based on the field metadata.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param field the metadata for the field to add, must not be {@code null}
     * @return SQL statement to add the column, never {@code null}
     */
    String getAddColumnSql(EntityMetadata metadata, FieldMetadata field);

    /**
     * Generates SQL to create an index.
     * <p>
     * This method should create a CREATE INDEX statement for the specified
     * columns. The implementation should handle unique indexes appropriately
     * and use proper syntax for the target database.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param index the index metadata containing name and columns, must not be {@code null}
     * @return SQL statement to create the index, never {@code null}
     */
    String getCreateIndexSql(EntityMetadata metadata, IndexMetadata index);

    /**
     * Generates SQL to drop an index.
     * <p>
     * This method should create a DROP INDEX statement using the appropriate
     * syntax for the target database. Some databases require specifying the
     * table name while others don't.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param index the index metadata containing the index name, must not be {@code null}
     * @return SQL statement to drop the index, never {@code null}
     */
    String getDropIndexSql(EntityMetadata metadata, IndexMetadata index);

    /**
     * Returns the database-specific SQL type for a generic column type.
     * <p>
     * This method maps generic column types to database-specific data types.
     * For example, ColumnType.STRING might map to VARCHAR(255) in MySQL
     * but TEXT in SQLite.
     * </p>
     *
     * @param type the generic column type, must not be {@code null}
     * @return the database-specific SQL type, never {@code null}
     */
    String getSqlType(ColumnType type);

    /**
     * Generates a LIMIT clause for query results.
     * <p>
     * This method should return the appropriate LIMIT clause syntax for the
     * target database. If limit is null, an empty string should be returned.
     * </p>
     *
     * @param limit the maximum number of results to return, may be {@code null}
     * @return the LIMIT clause, or empty string if limit is null
     */
    String getLimitClause(Integer limit);

    /**
     * Generates a SELECT query with optional WHERE clause and LIMIT.
     * <p>
     * This method should create a complete SELECT statement that retrieves
     * all columns from the entity table, optionally filtered by conditions
     * and limited to a specific number of results.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param conditions list of WHERE conditions, may be empty
     * @param params list to collect parameter values, may be empty
     * @param limit optional result limit, may be {@code null}
     * @return complete SELECT SQL statement, never {@code null}
     */
    String getSelectSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params, Integer limit);

    /**
     * Generates a DELETE query with optional WHERE clause.
     * <p>
     * This method should create a DELETE statement that removes rows from the
     * entity table, optionally filtered by conditions. If no conditions are
     * provided, this should delete all rows.
     * </p>
     *
     * @param metadata the entity metadata containing table information, must not be {@code null}
     * @param conditions list of WHERE conditions, may be empty
     * @param params list to collect parameter values, may be empty
     * @return complete DELETE SQL statement, never {@code null}
     */
    String getDeleteSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params);

    /**
     * Builds a SQL predicate fragment for a comparison operation.
     * <p>
     * This method should create a WHERE clause predicate comparing a column
     * to a value using the specified operator. The result should include
     * parameter placeholders rather than literal values.
     * </p>
     *
     * @param column the column name to compare, must not be {@code null}
     * @param operator the comparison operator, must not be {@code null}
     * @param value the value to compare against, may be {@code null}
     * @return a SqlFragment containing the predicate and parameters, never {@code null}
     */
    default SqlFragment buildPredicate(String column, Operator operator, Object value) {
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

        if(operator == Operator.IN) {
            final List<Object> values = new ArrayList<>();
            if(value instanceof Iterable) {
                for (Object item : (Iterable<?>) value) {values.add(item);}
            } else if(value.getClass().isArray()) {
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {values.add(Array.get(value, i));}
            } else {
                values.add(value);
            }

            if (values.isEmpty()) {
                return new SqlFragment("1=0", List.of()); // Always false
            }

            if(values.size() == 1) {
                return new SqlFragment(colSql + " = ?", List.of(values.get(0)));
            } else {
                String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
                return new SqlFragment(colSql + " IN (" + placeholders + ")", values);
            }
        }

        final String symbol = operator.getSymbol(); // "=", "!=", ">", "<", ">=", "<=", "LIKE"
        return new SqlFragment(colSql + " " + symbol + " ?", List.of(value));
    }

    default String buildWhereClauseAndCollectParams(List<SqlCondition> conditions, List<Object> params) {
        if (conditions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" WHERE ");
        boolean first = true;
        for (SqlCondition c : conditions) {
            if (!first && c.connector() != null) {
                sb.append(' ').append(c.connector()).append(' ');
            }
            sb.append(c.fragment().sql());
            params.addAll(c.fragment().params());
            first = false;
        }
        return sb.toString();
    }

    default String quoteIdentifier(String identifier) {
        if (identifier == null) return null;
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    default Object toDatabaseValue(Object v) {
        if (v == null) return null;

        if (v instanceof Character) return v.toString();
        if (v instanceof Enum<?>) return ((Enum<?>) v).name();
        if (v instanceof UUID) return v.toString();

        if (v instanceof Instant) return Timestamp.from((Instant) v);
        if (v instanceof LocalDateTime) return Timestamp.valueOf((LocalDateTime) v);
        if (v instanceof LocalDate) return java.sql.Date.valueOf((LocalDate) v);
        if (v instanceof LocalTime) return java.sql.Time.valueOf((LocalTime) v);
        if (v instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) v).getTime());
        }

        return v;
    }

    default Object fromDatabaseValue(Object v, Class<?> targetType) {
        if (targetType == null) return v;

        if (v == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == byte.class) return (byte) 0;
                if (targetType == short.class) return (short) 0;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == float.class) return 0f;
                if (targetType == double.class) return 0d;
                if (targetType == char.class) return (char) 0;
            }
            return null;
        }

        if (targetType.isInstance(v)) {
            return v;
        }

        // Numbers
        if (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) {
            Number n;
            if (v instanceof Number) {
                n = (Number) v;
            } else {
                String s = v.toString();
                try {
                    if (s.contains(".") || s.contains("e") || s.contains("E")) {
                        n = Double.parseDouble(s);
                    } else {
                        n = Long.parseLong(s);
                    }
                } catch (NumberFormatException e) {
                    n = 0;
                }
            }
            if (targetType == Byte.class || targetType == byte.class) return n.byteValue();
            if (targetType == Short.class || targetType == short.class) return n.shortValue();
            if (targetType == Integer.class || targetType == int.class) return n.intValue();
            if (targetType == Long.class || targetType == long.class) return n.longValue();
            if (targetType == Float.class || targetType == float.class) return n.floatValue();
            if (targetType == Double.class || targetType == double.class) return n.doubleValue();
        }

        // Boolean
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (v instanceof Number) {
                return ((Number) v).intValue() != 0;
            }
            return Boolean.parseBoolean(v.toString());
        }

        // Character
        if (targetType == Character.class || targetType == char.class) {
            String s = v.toString();
            return s.isEmpty() ? '\0' : s.charAt(0);
        }

        // Enum
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<? extends Enum> et = (Class<? extends Enum>) targetType;
            return Enum.valueOf(et, v.toString());
        }

        // UUID
        if (targetType == UUID.class) {
            return UUID.fromString(v.toString());
        }

        // java.time
        if (targetType == Instant.class) {
            if (v instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) v).toInstant();
            }
            return Instant.parse(v.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (v instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) v).toLocalDateTime();
            }
            return LocalDateTime.parse(v.toString());
        }
        if (targetType == LocalDate.class) {
            if (v instanceof java.sql.Date) {
                return ((java.sql.Date) v).toLocalDate();
            }
            return LocalDate.parse(v.toString());
        }
        if (targetType == LocalTime.class) {
            if (v instanceof java.sql.Time) {
                return ((java.sql.Time) v).toLocalTime();
            }
            return LocalTime.parse(v.toString());
        }

        if(v instanceof java.sql.Blob) {
            try {
                final byte[] bytes = ((java.sql.Blob) v).getBytes(1, (int) ((java.sql.Blob) v).length());
                final Object val = BlobSerializationUtility.deserialize(bytes, targetType);
                return targetType.cast(val);
            } catch (SQLException exception) {
                throw new RuntimeException("Failed to read BLOB data", exception);
            }
        }

        if (targetType == String.class) {
            return v.toString();
        }

        return targetType.cast(v);
    }
}
