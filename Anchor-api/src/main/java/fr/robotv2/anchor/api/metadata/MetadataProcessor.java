package fr.robotv2.anchor.api.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataProcessor {

    private static final Map<Class<?>, EntityMetadata> cache = new ConcurrentHashMap<>();

    private MetadataProcessor() {
        throw new UnsupportedOperationException("This class is a utility class and cannot be instantiated");
    }

    public static EntityMetadata getMetadata(Class<?> cls) {
        return cache.computeIfAbsent(cls, (ignored) -> EntityMetadata.create(cls));
    }
}
