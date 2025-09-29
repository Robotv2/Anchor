package fr.robotv2.anchor.sql.mapper;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import fr.robotv2.anchor.sql.dialect.SQLDialect;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class EntityRowMapper<T> implements RowMapper<T> {

    private final Class<T> cls;
    private final EntityMetadata metadata;
    private final SQLDialect dialect;

    public EntityRowMapper(Class<T> cls, EntityMetadata metadata, SQLDialect dialect) {
        this.cls = cls;
        this.metadata = metadata;
        this.dialect = dialect;
    }

    @Override
    public T map(ResultSet rs) throws SQLException {
        try {
            final Constructor<T> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            final T instance = ctor.newInstance();
            for(Map.Entry<String, FieldMetadata> entry : metadata.getAllFields().entrySet()) {
                setField(instance, rs, entry.getValue());
            }
            return instance;
        } catch (SQLException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SQLException("Failed to map row", exception);
        }
    }

    private void setField(T instance, ResultSet rs, FieldMetadata fm) throws Exception {
        final Object dbVal = rs.getObject(fm.getColumnName());
        final Object javaVal = dialect.fromDatabaseValue(dbVal, fm.getField().getType());
        fm.safeSet(instance, javaVal);
    }
}
