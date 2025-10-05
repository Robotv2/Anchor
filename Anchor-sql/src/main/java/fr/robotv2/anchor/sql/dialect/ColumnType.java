package fr.robotv2.anchor.sql.dialect;

import fr.robotv2.anchor.api.metadata.FieldMetadata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public enum ColumnType {

    BIGINT,
    BLOB,
    BOOLEAN,
    CHAR,
    DATE,
    DECIMAL,
    DOUBLE,
    FLOAT,
    INTEGER,
    TIMESTAMP,
    UUID,
    VARCHAR,
    TEXT,
    ;

    public static ColumnType fromJavaClass(Class<?> clazz, FieldMetadata fm) {
        if (clazz == null) return null;

        // Primitive and wrapper integers
        if (clazz == int.class || clazz == Integer.class) return INTEGER;
        if (clazz == long.class || clazz == Long.class) return BIGINT;
        if (clazz == short.class || clazz == Short.class) return INTEGER;
        if (clazz == byte.class || clazz == Byte.class) return INTEGER;

        // Floating point
        if (clazz == double.class || clazz == Double.class) return DOUBLE;
        if (clazz == float.class || clazz == Float.class) return FLOAT;

        // Boolean
        if (clazz == boolean.class || clazz == Boolean.class) return BOOLEAN;

        // Character
        if (clazz == char.class || clazz == Character.class) return CHAR;

        // Strings
        if (clazz == String.class) return VARCHAR;

        // Big numbers
        if (clazz == BigInteger.class) return BIGINT;
        if (clazz == BigDecimal.class) return DECIMAL;

        // UUID
        if (clazz == UUID.class) return UUID;

        // java.util.Date and java.sql.Date/Time/Timestamp
        if (java.sql.Timestamp.class.isAssignableFrom(clazz)) return TIMESTAMP;
        if (Date.class.isAssignableFrom(clazz)) return DATE;

        // java.time (recommend TIMESTAMP for date-times, DATE for LocalDate)
        if (LocalDateTime.class.isAssignableFrom(clazz)) return TIMESTAMP;
        if (Instant.class.isAssignableFrom(clazz)) return TIMESTAMP;
        if (LocalDate.class.isAssignableFrom(clazz)) return DATE;

        // Binary
        if (clazz == byte[].class || clazz == Byte[].class || fm.isBlob()) return BLOB;

        // Fallback to VARCHAR for unknown types
        return VARCHAR;
    }

    public boolean isNumeric() {
        return this == INTEGER || this == BIGINT || this == DOUBLE || this == FLOAT || this == DECIMAL;
    }

    public boolean isTextual() {
        return this == VARCHAR || this == TEXT || this == UUID || this == CHAR;
    }

    public boolean isTemporal() {
        return this == DATE || this == TIMESTAMP;
    }

    public boolean isBinary() {
        return this == BLOB;
    }
}
