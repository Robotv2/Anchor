package fr.robotv2.anchor.sql.dialect;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.api.metadata.IndexMetadata;
import fr.robotv2.anchor.api.repository.Operator;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface SQLDialect {

    String getCreateTableIfNotExists(EntityMetadata metadata);

    String getDropTableSql(EntityMetadata metadata);

    String getUpsertSql(EntityMetadata metadata);

    String getAddColumnSql(EntityMetadata metadata, FieldMetadata field);

    String getCreateIndexSql(EntityMetadata metadata, IndexMetadata index);

    String getDropIndexSql(EntityMetadata metadata, IndexMetadata index);

    String getSqlType(ColumnType type);

    String getLimitClause(Integer limit);

    String getSelectSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params, Integer limit);

    String getDeleteSql(EntityMetadata metadata, List<SqlCondition> conditions, List<Object> params);

    SqlFragment buildPredicate(String column, Operator operator, Object value);

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

        if (targetType == String.class) {
            return v.toString();
        }

        return targetType.cast(v);
    }
}
