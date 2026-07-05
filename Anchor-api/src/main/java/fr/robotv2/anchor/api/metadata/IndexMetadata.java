package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for a database index defined on an entity.
 * <p>
 * IndexMetadata encapsulates information about an index that should be created
 * to improve query performance. Indexes can be defined at the field level
 * (single-column indexes) or at the class level (composite indexes).
 * This metadata is used by database dialects to generate the appropriate
 * CREATE INDEX statements.
 * </p>
 *
 * @since 1.0
 * @see Index
 * @see EntityMetadata
 */
public class IndexMetadata {

    private final String name;
    private final List<String> columns;
    private final boolean unique;

    /**
     * Creates IndexMetadata with the specified properties.
     *
     * @param name the index name, must not be {@code null}
     * @param columns the list of column names to index, must not be {@code null} or empty
     * @param unique whether the index should enforce uniqueness
     */
    public IndexMetadata(String name, List<String> columns, boolean unique) {
        this.name = name;
        this.columns = columns;
        this.unique = unique;
    }

    /**
     * Creates IndexMetadata from an {@link Index} annotation.
     * <p>
     * This factory method processes the Index annotation and creates appropriate
     * metadata. If the annotation doesn't specify a name, the default name is used.
     * If no columns are specified in the annotation, the available columns are used.
     * </p>
     *
     * @param index the Index annotation to process, must not be {@code null}
     * @param defaultName the default index name to use if not specified in annotation
     * @param availableColumns the available column names (used for field-level indexes), must not be {@code null}
     * @return IndexMetadata created from the annotation
     */
    public static IndexMetadata fromAnnotation(Index index, String defaultName, List<String> availableColumns) {
        String name = index.name().isEmpty() ? defaultName : index.name();
        String[] columns = index.columns().length == 0 ? availableColumns.toArray(new String[0]) : index.columns();

        return new IndexMetadata(name, Arrays.asList(columns), index.unique());
    }

    /**
     * Returns the name of the index.
     * <p>
     * This name is used when creating the index in the database and should be
     * unique within the database schema.
     * </p>
     *
     * @return the index name, never {@code null}
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns an unmodifiable list of column names that are included in this index.
     * <p>
     * For single-column indexes, this list contains one element.
     * For composite indexes, it contains multiple columns in the order
     * they should be indexed.
     * </p>
     *
     * @return an unmodifiable list of column names, never {@code null} or empty
     */
    @NotNull
    @UnmodifiableView
    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Returns whether this index enforces uniqueness.
     * <p>
     * If {@code true}, the database will prevent duplicate values in the
     * indexed column(s). This is useful for creating unique constraints
     * on fields like email addresses or usernames.
     * </p>
     *
     * @return {@code true} if the index is unique, {@code false} otherwise
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Returns a comma-separated string of column names.
     * <p>
     * This is useful for generating SQL statements where columns need to be
     * listed as a comma-separated list, such as in CREATE INDEX statements.
     * </p>
     *
     * @return a string with column names separated by commas, never {@code null}
     */
    @NotNull
    public String getColumnList() {
        return String.join(", ", columns);
    }
}