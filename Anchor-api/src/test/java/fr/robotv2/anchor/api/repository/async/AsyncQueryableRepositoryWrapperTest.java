package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.QueryBuilder;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AsyncQueryableRepositoryWrapper.
 */
class AsyncQueryableRepositoryWrapperTest {

    private QueryableRepository<Long, TestEntity> mockRepository;
    private QueryBuilder<Long, TestEntity> mockQueryBuilder;
    private Executor executor;
    private AsyncQueryableRepositoryWrapper<Long, TestEntity> asyncWrapper;

    @BeforeEach
    void setUp() {
        mockRepository = mock(QueryableRepository.class);
        mockQueryBuilder = mock(QueryBuilder.class);
        executor = Executors.newSingleThreadExecutor();
        asyncWrapper = new AsyncQueryableRepositoryWrapper<>(mockRepository, executor);
    }

    @Test
    void testConstructorWithNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncQueryableRepositoryWrapper<>(null, executor);
        });
    }

    @Test
    void testConstructorWithNullExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncQueryableRepositoryWrapper<>(mockRepository, null);
        });
    }

    @Test
    void testQuery() {
        when(mockRepository.query()).thenReturn(mockQueryBuilder);

        AsyncQueryBuilder<Long, TestEntity> asyncQueryBuilder = asyncWrapper.query();

        assertNotNull(asyncQueryBuilder);
        verify(mockRepository, times(1)).query();
    }

    @Test
    void testGetQueryableRepository() {
        assertEquals(mockRepository, asyncWrapper.getQueryableRepository());
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
        QueryableRepository<Long, TestEntity> repo = mockRepository;
        AsyncQueryableRepository<Long, TestEntity> wrapped = AsyncQueryableRepository.wrap(repo, executor);
        
        assertNotNull(wrapped);
        assertTrue(wrapped instanceof AsyncQueryableRepositoryWrapper);
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
