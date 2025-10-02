package fr.robotv2.anchor.api.repository;

import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for entities that have a unique identifier.
 * <p>
 * All entity classes that can be persisted using the Anchor framework must
 * implement this interface. It provides a contract for accessing the entity's
 * unique identifier, which is used for database operations such as finding,
 * updating, and deleting entities.
 * </p>
 *
 * @param <ID> the type of the unique identifier (e.g., Long, String, UUID)
 * @since 1.0
 * @see Repository
 * @see AsyncRepository
 * @see fr.robotv2.anchor.api.annotation.Id
 */
public interface Identifiable<ID> {

    /**
     * Returns the unique identifier for this entity.
     * <p>
     * This identifier is used by the framework to distinguish between different
     * entities of the same type. It must be unique across all entities and
     * should remain stable throughout the entity's lifetime.
     * </p>
     *
     * @return the unique identifier, may be {@code null} for entities that haven't been persisted yet
     */
    @Nullable
    ID getId();
}
