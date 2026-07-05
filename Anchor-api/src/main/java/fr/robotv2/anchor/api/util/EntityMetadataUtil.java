package fr.robotv2.anchor.api.util;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.FieldMetadata;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for resolving field names to database column names.
 * <p>
 * This class provides helper methods for mapping between various ways of referring
 * to entity fields (Java field names, column names) and their corresponding
 * database column names. It's particularly useful in query builders and
 * metadata processing where field references need to be resolved to actual
 * database column names.
 * </p>
 *
 * @since 1.0
 * @see EntityMetadata
 * @see FieldMetadata
 */
public class EntityMetadataUtil {

    /**
     * Resolves a field reference to a database column name.
     * <p>
     * This method attempts to map various forms of field references to the
     * actual database column name. It supports Java field names, column annotation
     * values, and case-insensitive matching. If no match is found, the original
     * input is returned (treated as a raw column name).
     * </p>
     *
     * <p>The resolution follows this order:</p>
     * <ol>
     *   <li>Check if input matches a non-ID field's @Column value</li>
     *   <li>Check if input matches a non-ID field's Java field name (case-insensitive)</li>
     *   <li>Check if input matches the ID field's @Column value (case-insensitive)</li>
     *   <li>Check if input matches the ID field's Java field name (case-insensitive)</li>
     *   <li>Return the input as-is (assume it's a raw column name)</li>
     * </ol>
     *
     * @param metadata the entity metadata containing field mappings, must not be {@code null}
     * @param input the field reference to resolve, must not be {@code null}
     * @return the resolved database column name, never {@code null}
     * @throws IllegalArgumentException if metadata or input is {@code null}
     */
    @NotNull
    public static String resolveName(@NotNull EntityMetadata metadata, @NotNull String input) {
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
