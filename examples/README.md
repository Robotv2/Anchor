# Async Repository Examples

This directory contains practical examples demonstrating how to use and extend the new async repository wrapper architecture.

## Examples Included

### 1. CachedAsyncQueryableRepository.java
A custom async repository implementation that adds an in-memory cache layer.

**Features:**
- Configurable cache expiry time
- Automatic cache invalidation on updates/deletes
- Cache statistics tracking
- Zero-copy returns for cached values

**Usage:**
```java
QueryableRepository<Long, User> syncRepo = database.getRepository(User.class);
Executor executor = Executors.newFixedThreadPool(4);

CachedAsyncQueryableRepository<Long, User> cachedRepo = 
    new CachedAsyncQueryableRepository<>(syncRepo, executor, 5 * 60 * 1000L); // 5 min cache

// First call hits database
cachedRepo.findById(1L).thenAccept(user -> {
    System.out.println("From database");
});

// Second call uses cache (much faster!)
cachedRepo.findById(1L).thenAccept(user -> {
    System.out.println("From cache");
});

// Check stats
System.out.println(cachedRepo.getStats());
```

## Why These Examples Matter

Before the async repository architecture improvements, creating custom async repository implementations like these was **not possible** because:

1. Anonymous implementations couldn't be extended
2. No access to the underlying repository or executor
3. Code duplication would be required for each repository type

Now, with concrete wrapper classes, developers can:
- ✅ Extend wrapper classes to add custom behavior
- ✅ Access underlying repository and executor
- ✅ Create reusable middleware (caching, metrics, logging, etc.)
- ✅ Compose multiple wrappers for advanced functionality

## Creating Your Own Custom Wrapper

```java
public class MyCustomAsyncRepository<ID, E extends Identifiable<ID>> 
        extends AsyncRepositoryWrapper<ID, E> {
    
    public MyCustomAsyncRepository(Repository<ID, E> repository, Executor executor) {
        super(repository, executor);
    }
    
    @Override
    public CompletableFuture<Void> save(E entity) {
        // Add custom logic before save
        System.out.println("Saving: " + entity.getId());
        
        // Delegate to parent
        return super.save(entity)
                .whenComplete((result, error) -> {
                    // Add custom logic after save
                    if (error == null) {
                        System.out.println("Saved successfully!");
                    }
                });
    }
    
    // Override other methods as needed...
}
```

## Best Practices

1. **Always call super methods** - Delegate to parent implementation to maintain functionality
2. **Use thenApply/thenRun/whenComplete** - Add custom logic without blocking
3. **Handle errors gracefully** - Use exceptionally() or handle() for error handling
4. **Consider thread safety** - Use concurrent collections if maintaining state
5. **Document your extensions** - Explain what additional behavior your wrapper provides

## License

These examples are provided as-is for educational purposes.
