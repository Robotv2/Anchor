package fr.robotv2.anchor.api.util;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;

public class EntityMetadataUtil {

    public static String resolveName(EntityMetadata metadata, String input) {
        String in = input.trim();

        // non-id fields by @Column name
        FieldMetadata fmByColumn = metadata.getField(in);
        if (fmByColumn != null) {
            return fmByColumn.getColumnName();
        }

        // non-id fields by Java field name
        for (FieldMetadata fm : metadata.getFields().values()) {
            if (fm.getField().getName().equalsIgnoreCase(in)) {
                return fm.getColumnName();
            }
        }

        // id by column name or Java field name
        FieldMetadata idMeta = metadata.getIdField();
        if (idMeta.getColumnName().equalsIgnoreCase(in)) {
            return idMeta.getColumnName();
        }
        if (idMeta.getField().getName().equalsIgnoreCase(in)) {
            return idMeta.getColumnName();
        }

        // fallback: use as-is
        return in;
    }

}
