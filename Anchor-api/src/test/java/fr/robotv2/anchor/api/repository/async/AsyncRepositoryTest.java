package fr.robotv2.anchor.api.repository.async;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.database.SupportType;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.QueryBuilder;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.api.repository.Repository;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncRepositoryTest {

    @Test
    void wrapsEveryRepositoryOperation() {
        RecordingRepository repository = new RecordingRepository();
        CountingExecutor executor = new CountingExecutor();
        AsyncRepository<Integer, TestEntity> async = AsyncRepository.wrap(repository, executor);

        assertRepositoryOperations(async, repository);
        assertEquals(8, executor.count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesQueryableDelegationAndFluentChaining() {
        RecordingQueryableRepository repository = new RecordingQueryableRepository();
        CountingExecutor executor = new CountingExecutor();
        AsyncRepository<Integer, TestEntity> wrapped = AsyncRepository.wrap(repository, executor);
        AsyncQueryableRepository<Integer, TestEntity> async =
                assertInstanceOf(AsyncQueryableRepository.class, wrapped);

        assertRepositoryOperations(async, repository);
        AsyncQueryBuilder<Integer, TestEntity> query = async.query();
        assertSame(query, query.where("name", Operator.EQUAL, "Ada"));
        assertSame(query, query.and());
        assertSame(query, query.or());
        assertSame(query, query.limit(2));
        assertEquals("name", repository.builder.column);
        assertEquals(Operator.EQUAL, repository.builder.operator);
        assertEquals("Ada", repository.builder.value);
        assertEquals(1, repository.builder.andCalls);
        assertEquals(1, repository.builder.orCalls);
        assertEquals(2, repository.builder.limit);
        assertSame(repository.builder.allResult, query.all().join());
        assertSame(repository.builder.oneResult, query.one().join());
        assertEquals(repository.builder.deleteResult, query.delete().join());
        assertEquals(1, repository.queryCalls);
        assertEquals(11, executor.count);
    }

    private void assertRepositoryOperations(
            AsyncRepository<Integer, TestEntity> async,
            RecordingRepository repository
    ) {
        TestEntity first = new TestEntity(1);
        TestEntity second = new TestEntity(2);
        List<TestEntity> entities = List.of(first, second);
        List<Integer> ids = List.of(1, 2);

        async.save(first).join();
        repository.assertRecorded("save", first);
        async.saveAll(entities).join();
        repository.assertRecorded("saveAll", entities);
        async.delete(first).join();
        repository.assertRecorded("delete", first);
        async.deleteById(1).join();
        repository.assertRecorded("deleteById", 1);
        async.deleteAll(entities).join();
        repository.assertRecorded("deleteAll", entities);
        async.deleteAllById(ids).join();
        repository.assertRecorded("deleteAllById", ids);
        assertSame(repository.foundResult, async.findById(2).join());
        repository.assertRecorded("findById", 2);
        assertSame(repository.allResult, async.findAll().join());
        repository.assertRecorded("findAll", null);
    }

    @Test
    void completesFuturesExceptionallyWithOriginalCause() {
        RuntimeException repositoryFailure = new RuntimeException("repository failed");
        RecordingRepository repository = new RecordingRepository();
        repository.failure = repositoryFailure;
        CountingExecutor executor = new CountingExecutor();

        CompletableFuture<List<TestEntity>> repositoryFuture =
                AsyncRepository.wrap(repository, executor).findAll();
        CompletionException repositoryException =
                assertThrows(CompletionException.class, repositoryFuture::join);
        assertSame(repositoryFailure, repositoryException.getCause());

        RuntimeException queryFailure = new RuntimeException("query failed");
        RecordingQueryBuilder builder = new RecordingQueryBuilder();
        builder.failure = queryFailure;
        CompletableFuture<TestEntity> queryFuture = AsyncQueryBuilder.wrap(builder, executor).one();
        CompletionException queryException = assertThrows(CompletionException.class, queryFuture::join);
        assertSame(queryFailure, queryException.getCause());
        assertEquals(2, executor.count);
    }

    @Test
    void rejectsNullDelegatesAndExecutorsImmediately() {
        RecordingRepository repository = new RecordingRepository();
        RecordingQueryableRepository queryable = new RecordingQueryableRepository();
        RecordingQueryBuilder builder = new RecordingQueryBuilder();
        Executor executor = Runnable::run;

        assertThrows(NullPointerException.class, () -> AsyncRepository.wrap(null, executor));
        assertThrows(NullPointerException.class, () -> AsyncRepository.wrap(repository, null));
        assertThrows(NullPointerException.class, () -> AsyncQueryableRepository.wrap(null, executor));
        assertThrows(NullPointerException.class, () -> AsyncQueryableRepository.wrap(queryable, null));
        assertThrows(NullPointerException.class, () -> AsyncQueryBuilder.wrap(null, executor));
        assertThrows(NullPointerException.class, () -> AsyncQueryBuilder.wrap(builder, null));
    }

    @Test
    void databaseProvidesBothAsyncRepositoryOverloads() {
        RecordingRepository repository = new RecordingRepository();
        RecordingDatabase database = new RecordingDatabase(repository);
        CountingExecutor executor = new CountingExecutor();

        AsyncRepository<Integer, TestEntity> explicit =
                database.getAsyncRepository(TestEntity.class, executor);
        assertSame(repository.allResult, explicit.findAll().join());
        assertEquals(TestEntity.class, database.requestedClass);
        assertEquals(1, executor.count);

        AsyncRepository<Integer, TestEntity> defaultExecutor =
                database.getAsyncRepository(TestEntity.class);
        assertSame(repository.foundResult, defaultExecutor.findById(1).join());
        assertEquals(TestEntity.class, database.requestedClass);
    }

    private record TestEntity(Integer id) implements Identifiable<Integer> {

        @Override
        public Integer getId() {
            return id;
        }
    }

    private static class RecordingRepository implements Repository<Integer, TestEntity> {

        private final Optional<TestEntity> foundResult = Optional.of(new TestEntity(2));
        private final List<TestEntity> allResult = List.of(new TestEntity(1), new TestEntity(2));
        private RuntimeException failure;
        private String operation;
        private Object argument;

        @Override
        public void save(TestEntity entity) {
            record("save", entity);
        }

        @Override
        public void saveAll(Collection<TestEntity> entities) {
            record("saveAll", entities);
        }

        @Override
        public void delete(TestEntity entity) {
            record("delete", entity);
        }

        @Override
        public void deleteById(Integer id) {
            record("deleteById", id);
        }

        @Override
        public void deleteAll(Collection<TestEntity> entities) {
            record("deleteAll", entities);
        }

        @Override
        public void deleteAllById(Collection<Integer> ids) {
            record("deleteAllById", ids);
        }

        @Override
        public Optional<TestEntity> findById(Integer id) {
            record("findById", id);
            return foundResult;
        }

        @Override
        public List<TestEntity> findAll() {
            record("findAll", null);
            if(failure != null) {
                throw failure;
            }
            return allResult;
        }

        private void record(String operation, Object argument) {
            this.operation = operation;
            this.argument = argument;
        }

        private void assertRecorded(String expectedOperation, Object expectedArgument) {
            assertEquals(expectedOperation, operation);
            assertEquals(expectedArgument, argument);
        }
    }

    private static final class RecordingQueryableRepository extends RecordingRepository
            implements QueryableRepository<Integer, TestEntity> {

        private final RecordingQueryBuilder builder = new RecordingQueryBuilder();
        private int queryCalls;

        @Override
        public QueryBuilder<Integer, TestEntity> query() {
            queryCalls++;
            return builder;
        }
    }

    private static final class RecordingQueryBuilder implements QueryBuilder<Integer, TestEntity> {

        private final List<TestEntity> allResult = List.of(new TestEntity(1), new TestEntity(2));
        private final TestEntity oneResult = new TestEntity(1);
        private final int deleteResult = 2;
        private RuntimeException failure;
        private String column;
        private Operator operator;
        private Object value;
        private int andCalls;
        private int orCalls;
        private int limit;

        @Override
        public QueryBuilder<Integer, TestEntity> where(String column, Operator operator, Object value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
            return this;
        }

        @Override
        public QueryBuilder<Integer, TestEntity> and() {
            andCalls++;
            return this;
        }

        @Override
        public QueryBuilder<Integer, TestEntity> or() {
            orCalls++;
            return this;
        }

        @Override
        public QueryBuilder<Integer, TestEntity> limit(int count) {
            limit = count;
            return this;
        }

        @Override
        public List<TestEntity> all() {
            return allResult;
        }

        @Override
        public TestEntity one() {
            if(failure != null) {
                throw failure;
            }
            return oneResult;
        }

        @Override
        public int delete() {
            return deleteResult;
        }
    }

    private static final class CountingExecutor implements Executor {

        private int count;

        @Override
        public void execute(Runnable command) {
            count++;
            command.run();
        }
    }

    private static final class RecordingDatabase implements Database {

        private final Repository<Integer, TestEntity> repository;
        private Class<?> requestedClass;

        private RecordingDatabase(Repository<Integer, TestEntity> repository) {
            this.repository = repository;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean supports(SupportType type) {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <ID, T extends Identifiable<ID>> Repository<ID, T> getRepository(Class<T> clazz) {
            requestedClass = clazz;
            return (Repository<ID, T>) repository;
        }
    }
}
