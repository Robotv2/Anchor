package fr.robotv2.anchor.examples;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.api.repository.async.AsyncQueryableRepositoryWrapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Example: Custom async repository with caching layer.
 * 
 * This demonstrates how the new wrapper architecture allows developers to create
 * custom async repository implementations with additional features like caching,
 * without modifying the core framework.
 * 
 * Before: Not possible - had to use anonymous implementations
 * After: Easy to extend wrapper classes
 */
public class CachedAsyncQueryableRepository<ID, E extends Identifiable<ID>> 
        extends AsyncQueryableRepositoryWrapper<ID, E> {
    
    private final Map<ID, E> cache = new ConcurrentHashMap<>();
    private final long cacheExpiryMs;
    private final Map<ID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Creates a cached async repository.
     *
     * @param repository the underlying queryable repository
     * @param executor the executor for async operations
     * @param cacheExpiryMs cache expiry time in milliseconds
     */
    public CachedAsyncQueryableRepository(
            QueryableRepository<ID, E> repository,
            Executor executor,
            long cacheExpiryMs) {
        super(repository, executor);
        this.cacheExpiryMs = cacheExpiryMs;
    }
    
    @Override
    public CompletableFuture<java.util.Optional<E>> findById(ID id) {
        // Check if cache is valid
        E cached = cache.get(id);
        Long timestamp = cacheTimestamps.get(id);
        
        if (cached != null && timestamp != null) {
            long age = System.currentTimeMillis() - timestamp;
            if (age < cacheExpiryMs) {
                // Return cached value
                return CompletableFuture.completedFuture(java.util.Optional.of(cached));
            }
        }
        
        // Cache miss or expired - fetch from database
        return super.findById(id).thenApply(opt -> {
            opt.ifPresent(entity -> {
                cache.put(id, entity);
                cacheTimestamps.put(id, System.currentTimeMillis());
            });
            return opt;
        });
    }
    
    @Override
    public CompletableFuture<Void> save(E entity) {
        return super.save(entity).thenRun(() -> {
            // Update cache on save
            cache.put(entity.getId(), entity);
            cacheTimestamps.put(entity.getId(), System.currentTimeMillis());
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteById(ID id) {
        return super.deleteById(id).thenRun(() -> {
            // Invalidate cache on delete
            cache.remove(id);
            cacheTimestamps.remove(id);
        });
    }
    
    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        cache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Invalidates a specific cache entry.
     */
    public void invalidate(ID id) {
        cache.remove(id);
        cacheTimestamps.remove(id);
    }
    
    /**
     * Gets cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(
                cache.size(),
                cacheTimestamps.values().stream()
                        .filter(ts -> System.currentTimeMillis() - ts < cacheExpiryMs)
                        .count()
        );
    }
    
    /**
     * Cache statistics holder.
     */
    public static class CacheStats {
        private final long totalEntries;
        private final long validEntries;
        
        public CacheStats(long totalEntries, long validEntries) {
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
        }
        
        public long getTotalEntries() {
            return totalEntries;
        }
        
        public long getValidEntries() {
            return validEntries;
        }
        
        public long getExpiredEntries() {
            return totalEntries - validEntries;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, valid=%d, expired=%d}",
                    totalEntries, validEntries, getExpiredEntries());
        }
    }
}
