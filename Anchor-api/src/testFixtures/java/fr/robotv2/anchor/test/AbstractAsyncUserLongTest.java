package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.api.repository.async.AsyncQueryableRepository;
import fr.robotv2.anchor.api.repository.async.AsyncRepository;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract test class for testing asynchronous repository operations.
 * <p>
 * This test suite verifies that async operations work correctly and are thread-safe
 * across all database implementations.
 * </p>
 */
public abstract class AbstractAsyncUserLongTest {

    @TempDir
    Path tempDir;

    protected Database database;
    protected Repository<Long, UserLong> repository;
    protected AsyncRepository<Long, UserLong> asyncRepository;

    protected abstract Database createDatabase(Path tempDir);

    protected void onRepositoryReady(Repository<?, ?> repository) {}

    protected void onTearDown(Database database, Repository<Long, UserLong> repository) { }

    @BeforeEach
    void setUp() {
        database = createDatabase(tempDir);
        database.connect();
        repository = database.getRepository(UserLong.class);
        onRepositoryReady(repository);
        
        // Get async repository with custom executor for better control
        ExecutorService executor = Executors.newFixedThreadPool(4);
        asyncRepository = database.getAsyncRepository(UserLong.class, executor);
        
        // Setup test data
        repository.save(new UserLong(1L, "Alice", 30, true, "admin", "Ally"));
        repository.save(new UserLong(2L, "Bob", 25, false, "user", null));
        repository.save(new UserLong(3L, "Charlie", 35, true, "moderator", "Chuck"));
    }

    @AfterEach
    void tearDown() {
        onTearDown(database, repository);
        if (database != null) {
            database.disconnect();
        }
    }

    @Test
    void testAsyncFindById() throws Exception {
        CompletableFuture<Optional<UserLong>> future = asyncRepository.findById(1L);
        Optional<UserLong> userOpt = future.get(5, TimeUnit.SECONDS);
        
        Assertions.assertTrue(userOpt.isPresent());
        UserLong user = userOpt.get();
        Assertions.assertEquals("Alice", user.getName());
        Assertions.assertEquals(30, user.getAge());
    }

    @Test
    void testAsyncFindAll() throws Exception {
        CompletableFuture<List<UserLong>> future = asyncRepository.findAll();
        List<UserLong> users = future.get(5, TimeUnit.SECONDS);
        
        Assertions.assertNotNull(users);
        Assertions.assertEquals(3, users.size());
    }

    @Test
    void testAsyncSave() throws Exception {
        UserLong newUser = new UserLong(4L, "David", 28, true, "user", null);
        CompletableFuture<Void> saveFuture = asyncRepository.save(newUser);
        saveFuture.get(5, TimeUnit.SECONDS);
        
        // Verify the save worked
        Optional<UserLong> found = repository.findById(4L);
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("David", found.get().getName());
    }

    @Test
    void testAsyncSaveAll() throws Exception {
        List<UserLong> newUsers = Arrays.asList(
            new UserLong(10L, "User10", 20, true, "user", null),
            new UserLong(11L, "User11", 21, true, "user", null),
            new UserLong(12L, "User12", 22, true, "user", null)
        );
        
        CompletableFuture<Void> future = asyncRepository.saveAll(newUsers);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify all were saved
        List<UserLong> allUsers = repository.findAll();
        Assertions.assertEquals(6, allUsers.size());
    }

    @Test
    void testAsyncDelete() throws Exception {
        UserLong user = repository.findById(1L).orElseThrow();
        CompletableFuture<Void> future = asyncRepository.delete(user);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify deletion
        Optional<UserLong> found = repository.findById(1L);
        Assertions.assertFalse(found.isPresent());
    }

    @Test
    void testAsyncDeleteById() throws Exception {
        CompletableFuture<Void> future = asyncRepository.deleteById(2L);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify deletion
        Optional<UserLong> found = repository.findById(2L);
        Assertions.assertFalse(found.isPresent());
    }

    @Test
    void testAsyncDeleteAll() throws Exception {
        List<UserLong> toDelete = Arrays.asList(
            repository.findById(1L).orElseThrow(),
            repository.findById(2L).orElseThrow()
        );
        
        CompletableFuture<Void> future = asyncRepository.deleteAll(toDelete);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify deletions
        List<UserLong> remaining = repository.findAll();
        Assertions.assertEquals(1, remaining.size());
        Assertions.assertEquals(3L, remaining.get(0).getId());
    }

    @Test
    void testAsyncDeleteAllById() throws Exception {
        List<Long> ids = Arrays.asList(1L, 3L);
        CompletableFuture<Void> future = asyncRepository.deleteAllById(ids);
        future.get(5, TimeUnit.SECONDS);
        
        // Verify deletions
        List<UserLong> remaining = repository.findAll();
        Assertions.assertEquals(1, remaining.size());
        Assertions.assertEquals(2L, remaining.get(0).getId());
    }

    @Test
    void testAsyncQueryAll() throws Exception {
        Assumptions.assumeTrue(asyncRepository instanceof AsyncQueryableRepository, 
            "Repository does not support async querying");
        
        AsyncQueryableRepository<Long, UserLong> queryable = (AsyncQueryableRepository<Long, UserLong>) asyncRepository;
        CompletableFuture<List<UserLong>> future = queryable.query()
            .where("active", Operator.EQUAL, true)
            .all();
        
        List<UserLong> activeUsers = future.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(activeUsers);
        Assertions.assertEquals(2, activeUsers.size());
        Assertions.assertTrue(activeUsers.stream().allMatch(UserLong::getActive));
    }

    @Test
    void testAsyncQueryOne() throws Exception {
        Assumptions.assumeTrue(asyncRepository instanceof AsyncQueryableRepository, 
            "Repository does not support async querying");
        
        AsyncQueryableRepository<Long, UserLong> queryable = (AsyncQueryableRepository<Long, UserLong>) asyncRepository;
        CompletableFuture<UserLong> future = queryable.query()
            .where("name", Operator.EQUAL, "Bob")
            .one();
        
        UserLong user = future.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("Bob", user.getName());
    }

    @Test
    void testAsyncQueryDelete() throws Exception {
        Assumptions.assumeTrue(asyncRepository instanceof AsyncQueryableRepository, 
            "Repository does not support async querying");
        
        AsyncQueryableRepository<Long, UserLong> queryable = (AsyncQueryableRepository<Long, UserLong>) asyncRepository;
        CompletableFuture<Integer> future = queryable.query()
            .where("active", Operator.EQUAL, false)
            .delete();
        
        Integer deletedCount = future.get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(1, deletedCount);
        
        // Verify deletion
        List<UserLong> remaining = repository.findAll();
        Assertions.assertEquals(2, remaining.size());
    }

    @Test
    void testAsyncQueryWithLimit() throws Exception {
        Assumptions.assumeTrue(asyncRepository instanceof AsyncQueryableRepository, 
            "Repository does not support async querying");
        
        AsyncQueryableRepository<Long, UserLong> queryable = (AsyncQueryableRepository<Long, UserLong>) asyncRepository;
        CompletableFuture<List<UserLong>> future = queryable.query()
            .limit(2)
            .all();
        
        List<UserLong> users = future.get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(users);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void testConcurrentAsyncOperations() throws Exception {
        // Create multiple users concurrently
        int userCount = 20;
        List<CompletableFuture<Void>> futures = IntStream.range(100, 100 + userCount)
            .mapToObj(i -> new UserLong((long) i, "User" + i, 20 + (i % 30), i % 2 == 0, "user", null))
            .map(asyncRepository::save)
            .collect(Collectors.toList());
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(10, TimeUnit.SECONDS);
        
        // Verify all were saved
        List<UserLong> allUsers = repository.findAll();
        Assertions.assertTrue(allUsers.size() >= userCount + 3, 
            "Expected at least " + (userCount + 3) + " users, but got " + allUsers.size());
    }

    @Test
    void testConcurrentAsyncQueries() throws Exception {
        Assumptions.assumeTrue(asyncRepository instanceof AsyncQueryableRepository, 
            "Repository does not support async querying");
        
        AsyncQueryableRepository<Long, UserLong> queryable = (AsyncQueryableRepository<Long, UserLong>) asyncRepository;
        
        // Execute multiple queries concurrently
        List<CompletableFuture<List<UserLong>>> queryFutures = IntStream.range(0, 10)
            .mapToObj(i -> queryable.query()
                .where("active", Operator.EQUAL, true)
                .all())
            .collect(Collectors.toList());
        
        // Wait for all queries to complete
        CompletableFuture.allOf(queryFutures.toArray(new CompletableFuture[0]))
            .get(10, TimeUnit.SECONDS);
        
        // Verify all queries returned the same results
        for (CompletableFuture<List<UserLong>> future : queryFutures) {
            List<UserLong> result = future.get();
            Assertions.assertEquals(2, result.size());
            Assertions.assertTrue(result.stream().allMatch(UserLong::getActive));
        }
    }

    @Test
    void testAsyncOperationChaining() throws Exception {
        // Chain multiple async operations
        CompletableFuture<Optional<UserLong>> chainedFuture = asyncRepository
            .save(new UserLong(50L, "NewUser", 25, true, "user", null))
            .thenCompose(v -> asyncRepository.findById(50L))
            .thenApply(opt -> {
                opt.ifPresent(user -> user.setAge(26));
                return opt;
            })
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    return asyncRepository.save(opt.get())
                        .thenApply(v -> opt);
                }
                return CompletableFuture.completedFuture(opt);
            })
            .thenCompose(v -> asyncRepository.findById(50L));
        
        Optional<UserLong> result = chainedFuture.get(10, TimeUnit.SECONDS);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(26, result.get().getAge());
    }

    @Test
    void testAsyncErrorHandling() {
        // Test that async operations properly propagate exceptions
        CompletableFuture<Void> future = asyncRepository.save(null);
        
        ExecutionException exception = Assertions.assertThrows(
            ExecutionException.class,
            () -> future.get(5, TimeUnit.SECONDS)
        );
        
        Assertions.assertInstanceOf(NullPointerException.class, exception.getCause());
    }
}
