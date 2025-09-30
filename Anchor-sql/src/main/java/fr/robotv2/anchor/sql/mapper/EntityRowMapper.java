package fr.robotv2.anchor.sql.mapper;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.sql.dialect.SQLDialect;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EntityRowMapper<T> implements RowMapper<T> {

    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final SQLDialect dialect;

    private volatile MapperPlan<T> mapperPlan;

    public EntityRowMapper(Class<T> cls, EntityMetadata metadata, SQLDialect dialect) {
        this.cls = cls;
        this.metadata = metadata;
        this.dialect = dialect;
    }

    private record MapperPlan<T>(Supplier<T> instanceSupplier, List<FieldSetter<T>> fieldSetters) {}

    private record FieldSetter<T>(MethodHandle setter, Class<?> targetType, int columnIndex) {}

    @Override
    public T map(ResultSet rs) throws SQLException {
        if (mapperPlan == null) {
            synchronized (this) {
                if (mapperPlan == null) {
                    this.mapperPlan = createMapperPlan(rs.getMetaData());
                }
            }
        }

        try {
            final T instance = mapperPlan.instanceSupplier().get();
            for (final FieldSetter<T> fieldSetter : mapperPlan.fieldSetters()) {
                final Object dbVal = rs.getObject(fieldSetter.columnIndex());
                final Object javaVal = dialect.fromDatabaseValue(dbVal, fieldSetter.targetType());
                fieldSetter.setter().invoke(instance, javaVal);
            }
            return instance;
        } catch (Throwable e) {
            throw new SQLException("Failed to map row for entity " + cls.getName(), e);
        }
    }

    private MapperPlan<T> createMapperPlan(ResultSetMetaData rsmd) throws SQLException {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(cls, MethodHandles.lookup());
            final Supplier<T> instanceSupplier = createInstanceSupplier(lookup);
            final Map<String, Integer> columnIndexes = getColumnIndexMap(rsmd);
            final List<FieldSetter<T>> fieldSetters = new ArrayList<>();
            for (final FieldMetadata fm : metadata.getAllFields().values()) {
                final String columnName = fm.getColumnName();
                final Integer columnIndex = columnIndexes.get(columnName.toLowerCase());
                if (columnIndex != null) {
                    final MethodHandle setter = lookup.unreflectSetter(fm.getField());
                    fieldSetters.add(new FieldSetter<>(setter, fm.getField().getType(), columnIndex));
                }
            }
            return new MapperPlan<>(instanceSupplier, fieldSetters);
        } catch (Throwable e) {
            throw new SQLException("Failed to create a mapping plan for " + cls.getName(), e);
        }
    }

    private Map<String, Integer> getColumnIndexMap(ResultSetMetaData rsmd) throws SQLException {
        final Map<String, Integer> map = new java.util.HashMap<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            map.put(rsmd.getColumnLabel(i).toLowerCase(), i);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Supplier<T> createInstanceSupplier(MethodHandles.Lookup lookup) throws Throwable {
        final MethodHandle constructorHandle = lookup.findConstructor(cls, MethodType.methodType(void.class));
        return (Supplier<T>) LambdaMetafactory.metafactory(
                lookup,
                "get",
                MethodType.methodType(Supplier.class),
                MethodType.methodType(Object.class),
                constructorHandle,
                MethodType.methodType(cls)
                )
                .getTarget()
                .invoke();
    }
}
