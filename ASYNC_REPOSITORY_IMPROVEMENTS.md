# Async Repository Architecture Improvements

## Problem Statement

Previously, all async repositories were created using anonymous implementations via the `AsyncRepository.wrap()` method. This approach had several limitations:

1. **Breaking Abstraction**: Repository implementations couldn't provide their own optimized async versions
2. **Code Duplication**: Each repository variation needed duplicate wrapping logic
3. **Limited Extensibility**: No way to customize async behavior for specific repository types
4. **Anonymous Classes**: Hard to debug and maintain

## Solution: Concrete Wrapper Classes

We've introduced two new concrete wrapper classes that replace the anonymous implementations:

- `AsyncRepositoryWrapper<ID, E>` - Base wrapper for any `Repository`
- `AsyncQueryableRepositoryWrapper<ID, E>` - Wrapper for `QueryableRepository` with query support

### Benefits

1. **Better Abstraction**: Repository implementations can now extend wrapper classes
2. **No Code Duplication**: Single implementation shared across all repository types
3. **Extensibility**: Easy to customize async behavior per repository type
4. **Type Safety**: Concrete classes provide better type checking
5. **Backward Compatible**: Existing code continues to work without changes

## Usage Examples

### Basic Usage (No Changes Required)

Existing code continues to work without modifications:

```java
Database database = new MongoDBDatabase(config);
database.connect();

// Synchronous repository
Repository<UUID, User> syncRepo = database.getRepository(User.class);

// Async repository (same as before)
AsyncRepository<UUID, User> asyncRepo = database.getAsyncRepository(User.class);

// Usage remains the same
asyncRepo.findById(userId).thenAccept(userOpt -> {
    userOpt.ifPresent(user -> System.out.println("Found: " + user.getName()));
});
```

### Advanced: Custom Async Repository Implementation

Now repository implementations can provide optimized async versions:

```java
public class OptimizedMongoDBAsyncRepository<ID, E extends Identifiable<ID>> 
        extends AsyncQueryableRepositoryWrapper<ID, E> {
    
    private final MongoCollection<Document> collection;
    
    public OptimizedMongoDBAsyncRepository(
            QueryableRepository<ID, E> repository, 
            Executor executor,
            MongoCollection<Document> collection) {
        super(repository, executor);
        this.collection = collection;
    }
    
    @Override
    public CompletableFuture<Optional<E>> findById(ID id) {
        // Use MongoDB's native async driver for better performance
        return CompletableFuture.supplyAsync(() -> {
            // Custom implementation using async MongoDB operations
            // ...
        }, executor);
    }
}
```

### Extension Example: Add Caching Layer

```java
public class CachedAsyncRepository<ID, E extends Identifiable<ID>> 
        extends AsyncRepositoryWrapper<ID, E> {
    
    private final Map<ID, E> cache = new ConcurrentHashMap<>();
    
    public CachedAsyncRepository(Repository<ID, E> repository, Executor executor) {
        super(repository, executor);
    }
    
    @Override
    public CompletableFuture<Optional<E>> findById(ID id) {
        // Check cache first
        E cached = cache.get(id);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        
        // Delegate to wrapped repository
        return super.findById(id).thenApply(opt -> {
            opt.ifPresent(entity -> cache.put(id, entity));
            return opt;
        });
    }
    
    @Override
    public CompletableFuture<Void> save(E entity) {
        return super.save(entity).thenRun(() -> {
            cache.put(entity.getId(), entity);
        });
    }
}
```

## Architecture Improvements

### Before: Anonymous Implementation

```java
static <ID, E extends Identifiable<ID>> AsyncRepository<ID, E> wrap(
        Repository<ID, E> repository, Executor executor) {
    return new AsyncRepository<>() {
        @Override
        public CompletableFuture<Void> save(E entity) {
            return CompletableFuture.runAsync(() -> repository.save(entity), executor);
        }
        // ... 7 more methods with duplicated logic
    };
}
```

**Issues:**
- Cannot extend or customize
- Code duplication for QueryableRepository variant
- Hard to debug (anonymous class)
- No way to access underlying repository

### After: Concrete Wrapper Class

```java
public class AsyncRepositoryWrapper<ID, E extends Identifiable<ID>> 
        implements AsyncRepository<ID, E> {
    
    protected final Repository<ID, E> repository;
    protected final Executor executor;
    
    public AsyncRepositoryWrapper(Repository<ID, E> repository, Executor executor) {
        // ... validation
        this.repository = repository;
        this.executor = executor;
    }
    
    @Override
    public CompletableFuture<Void> save(E entity) {
        return CompletableFuture.runAsync(() -> repository.save(entity), executor);
    }
    
    // ... other methods
    
    // Getters for extension points
    public Repository<ID, E> getRepository() { return repository; }
    public Executor getExecutor() { return executor; }
}
```

**Benefits:**
- Can be extended and customized
- Single implementation (no duplication)
- Easy to debug (concrete class)
- Access to underlying repository and executor

## Migration Guide

**No migration needed!** The changes are 100% backward compatible.

Existing code using `AsyncRepository.wrap()` or `database.getAsyncRepository()` will continue to work without any modifications. The methods now return concrete wrapper classes instead of anonymous implementations, which provides the same functionality with added extensibility.

## Testing

Comprehensive unit tests have been added:

- `AsyncRepositoryWrapperTest` - Tests all CRUD operations
- `AsyncQueryableRepositoryWrapperTest` - Tests query builder integration

All existing integration tests pass without modifications, confirming backward compatibility.

## Future Possibilities

With this improved architecture, repository implementations can now:

1. **Provide Native Async Support**: MongoDB driver has native async support - repositories can use it directly
2. **Add Middleware**: Logging, metrics, caching can be added by extending wrapper classes
3. **Optimize Batch Operations**: Custom implementations can optimize bulk operations
4. **Add Retry Logic**: Extend wrappers to add automatic retry on failure
5. **Connection Pooling**: Better async connection management per repository type
