package fr.robotv2.anchor.api.metadata;

import fr.robotv2.anchor.api.annotation.Index;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IndexMetadata {

    private final String name;
    private final List<String> columns;
    private final boolean unique;

    public IndexMetadata(String name, List<String> columns, boolean unique) {
        this.name = name;
        this.columns = columns;
        this.unique = unique;
    }

    public static IndexMetadata fromAnnotation(Index index, String defaultName, List<String> availableColumns) {
        String name = index.name().isEmpty() ? defaultName : index.name();
        String[] columns = index.columns().length == 0 ? availableColumns.toArray(new String[0]) : index.columns();

        return new IndexMetadata(name, Arrays.asList(columns), index.unique());
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getColumnList() {
        return String.join(", ", columns);
    }
}