package fr.robotv2.anchor.sql.mapper;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldAccessor;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.sql.dialect.SQLDialect;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EntityRowMapper<T> implements RowMapper<T> {

    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final SQLDialect dialect;

    private volatile MapperPlan mapperPlan;

    public EntityRowMapper(Class<T> cls, EntityMetadata metadata, SQLDialect dialect) {
        this.cls = cls;
        this.metadata = metadata;
        this.dialect = dialect;
    }

    private record MapperPlan(List<FieldSetter> fieldSetters) {}

    private record FieldSetter(FieldAccessor accessor, int columnIndex) {}

    @Override
    public T map(ResultSet rs) throws SQLException {

        // Double-checked locking for thread-safe lazy initialization
        if (mapperPlan == null) {
            synchronized (this) {
                if (mapperPlan == null) {
                    this.mapperPlan = createMapperPlan(rs.getMetaData());
                }
            }
        }

        try {
            final T instance = metadata.newInstance();
            for (final FieldSetter setter : mapperPlan.fieldSetters()) {
                final Object dbVal = rs.getObject(setter.columnIndex());
                final Object javaVal = dialect.fromDatabaseValue(dbVal, setter.accessor().getFieldType());
                setter.accessor().set(instance, javaVal);
            }
            return instance;
        } catch (Throwable throwable) {
            throw new SQLException("Failed to map row for entity " + cls.getName(), throwable);
        }
    }

    private MapperPlan createMapperPlan(ResultSetMetaData rsmd) throws SQLException {
        final Map<String, Integer> columnIndexes = getColumnIndexMap(rsmd);
        final List<FieldSetter> fieldSetters = new ArrayList<>();

        for (final FieldMetadata fm : metadata.getAllFields().values()) {
            final String columnName = fm.getColumnName();
            final Integer columnIndex = columnIndexes.get(columnName.toLowerCase());
            if (columnIndex != null) {
                fieldSetters.add(new FieldSetter(fm.getAccessor(), columnIndex));
            }
        }
        return new MapperPlan(fieldSetters);
    }

    private Map<String, Integer> getColumnIndexMap(ResultSetMetaData rsmd) throws SQLException {
        final Map<String, Integer> map = new HashMap<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            map.put(rsmd.getColumnLabel(i).toLowerCase(), i);
        }
        return map;
    }
}
