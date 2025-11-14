package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncRepositoryWrapper.
 */
class AsyncRepositoryWrapperTest {

    private Repository<Long, TestEntity> mockRepository;
    private Executor executor;
    private AsyncRepositoryWrapper<Long, TestEntity> asyncWrapper;

    @BeforeEach
    void setUp() {
        mockRepository = mock(Repository.class);
        executor = Executors.newSingleThreadExecutor();
        asyncWrapper = new AsyncRepositoryWrapper<>(mockRepository, executor);
    }

    @Test
    void testConstructorWithNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncRepositoryWrapper<>(null, executor);
        });
    }

    @Test
    void testConstructorWithNullExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncRepositoryWrapper<>(mockRepository, null);
        });
    }

    @Test
    void testSave() throws ExecutionException, InterruptedException {
        TestEntity entity = new TestEntity(1L, "Test");
        doNothing().when(mockRepository).save(entity);

        CompletableFuture<Void> future = asyncWrapper.save(entity);
        future.get(); // Wait for completion

        verify(mockRepository, times(1)).save(entity);
    }

    @Test
    void testSaveAll() throws ExecutionException, InterruptedException {
        Collection<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, "Test1"),
                new TestEntity(2L, "Test2")
        );
        doNothing().when(mockRepository).saveAll(entities);

        CompletableFuture<Void> future = asyncWrapper.saveAll(entities);
        future.get();

        verify(mockRepository, times(1)).saveAll(entities);
    }

    @Test
    void testDelete() throws ExecutionException, InterruptedException {
        TestEntity entity = new TestEntity(1L, "Test");
        doNothing().when(mockRepository).delete(entity);

        CompletableFuture<Void> future = asyncWrapper.delete(entity);
        future.get();

        verify(mockRepository, times(1)).delete(entity);
    }

    @Test
    void testDeleteById() throws ExecutionException, InterruptedException {
        Long id = 1L;
        doNothing().when(mockRepository).deleteById(id);

        CompletableFuture<Void> future = asyncWrapper.deleteById(id);
        future.get();

        verify(mockRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteAll() throws ExecutionException, InterruptedException {
        Collection<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, "Test1"),
                new TestEntity(2L, "Test2")
        );
        doNothing().when(mockRepository).deleteAll(entities);

        CompletableFuture<Void> future = asyncWrapper.deleteAll(entities);
        future.get();

        verify(mockRepository, times(1)).deleteAll(entities);
    }

    @Test
    void testDeleteAllById() throws ExecutionException, InterruptedException {
        Collection<Long> ids = Arrays.asList(1L, 2L, 3L);
        doNothing().when(mockRepository).deleteAllById(ids);

        CompletableFuture<Void> future = asyncWrapper.deleteAllById(ids);
        future.get();

        verify(mockRepository, times(1)).deleteAllById(ids);
    }

    @Test
    void testFindById() throws ExecutionException, InterruptedException {
        Long id = 1L;
        TestEntity entity = new TestEntity(id, "Test");
        when(mockRepository.findById(id)).thenReturn(Optional.of(entity));

        CompletableFuture<Optional<TestEntity>> future = asyncWrapper.findById(id);
        Optional<TestEntity> result = future.get();

        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(mockRepository, times(1)).findById(id);
    }

    @Test
    void testFindAll() throws ExecutionException, InterruptedException {
        List<TestEntity> entities = Arrays.asList(
                new TestEntity(1L, "Test1"),
                new TestEntity(2L, "Test2")
        );
        when(mockRepository.findAll()).thenReturn(entities);

        CompletableFuture<List<TestEntity>> future = asyncWrapper.findAll();
        List<TestEntity> result = future.get();

        assertEquals(entities.size(), result.size());
        verify(mockRepository, times(1)).findAll();
    }

    @Test
    void testGetRepository() {
        assertEquals(mockRepository, asyncWrapper.getRepository());
    }

    @Test
    void testGetExecutor() {
        assertEquals(executor, asyncWrapper.getExecutor());
    }

    @Test
    void testWrapWithQueryableRepository() {
        Repository<Long, TestEntity> repo = mockRepository;
        AsyncRepository<Long, TestEntity> wrapped = AsyncRepository.wrap(repo, executor);
        
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof AsyncRepositoryWrapper);
    }

    /**
     * Simple test entity for testing purposes.
     */
    static class TestEntity implements Identifiable<Long> {
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
