package fr.robotv2.anchor.api.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for processing and caching entity metadata.
 * <p>
 * MetadataProcessor provides a centralized way to obtain {@link EntityMetadata}
 * for entity classes with built-in caching to improve performance. The metadata
 * is computed once per entity class and cached for subsequent requests, avoiding
 * the overhead of reflection-based analysis on every access.
 * </p>
 *
 * @since 1.0
 * @see EntityMetadata
 */
public class MetadataProcessor {

    private static final Map<Class<?>, EntityMetadata> cache = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This is a utility class and should not be instantiated. Attempting to
     * instantiate will throw an UnsupportedOperationException.
     * </p>
     *
     * @throws UnsupportedOperationException always thrown when called
     */
    private MetadataProcessor() {
        throw new UnsupportedOperationException("This class is a utility class and cannot be instantiated");
    }

    /**
     * Returns cached EntityMetadata for the specified entity class.
     * <p>
     * This method provides thread-safe access to entity metadata with automatic
     * caching. The first call for a particular class will compute the metadata
     * using {@link EntityMetadata#create(Class)} and cache the result. Subsequent
     * calls will return the cached instance, improving performance.
     * </p>
     *
     * <p>The caching is thread-safe and ensures that metadata is computed at most
     * once per entity class, even under concurrent access.</p>
     *
     * @param cls the entity class to get metadata for, must not be {@code null}
     * @return cached EntityMetadata for the specified class, never {@code null}
     * @throws IllegalArgumentException if cls is {@code null}
     * @throws IllegalArgumentException if the class is not properly annotated as an entity
     * @see EntityMetadata#create(Class)
     */
    @NotNull
    public static EntityMetadata getMetadata(@NotNull Class<?> cls) {
        return cache.computeIfAbsent(cls, (ignored) -> EntityMetadata.create(cls));
    }
}
