package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.MigrationExecutor;
import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.Queryable;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.test.model.UserLong;
import fr.robotv2.anchor.test.model.UserLongAdd;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

public abstract class AbstractUserLongTest {

    @TempDir
    Path tempDir;

    protected Database database;
    protected Repository<Long, UserLong> repository;

    protected abstract Database createDatabase(Path tempDir);

    protected void onRepositoryReady(Repository<Long, UserLong> repository) {}

    protected void onTearDown(Database database, Repository<Long, UserLong> repository) { }

    @BeforeEach
    void setUp() {
        database = createDatabase(tempDir);
        database.connect();
        repository = database.getRepository(UserLong.class);
        onRepositoryReady(repository);
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
    void testCreateTableAndUpsertAndSelectAll() {
        List<UserLong> all = repository.findAll();

        Assertions.assertNotNull(all);
        Assertions.assertEquals(3, all.size());
        Assertions.assertTrue(all.stream().anyMatch(u -> u.getId() == 1L));
        Assertions.assertTrue(all.stream().anyMatch(u -> u.getId() == 2L));
        Assertions.assertTrue(all.stream().anyMatch(u -> u.getId() == 3L));
    }

    @Test
    void testFindById() {
        UserLong user = repository.findById(2L).orElse(null);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("Bob", user.getName());
        Assertions.assertEquals(25, user.getAge());
        Assertions.assertFalse(user.getActive());
        Assertions.assertEquals("user", user.getGroupName());
        Assertions.assertNull(user.getNickname());
    }

    @Test
    void testUpdateAndFind() {
        UserLong user = repository.findById(1L).orElse(null);
        Assertions.assertNotNull(user);
        user.setAge(31);
        user.setNickname("Alicia");
        repository.save(user);

        UserLong updatedUser = repository.findById(1L).orElse(null);
        Assertions.assertNotNull(updatedUser);
        Assertions.assertEquals(31, updatedUser.getAge());
        Assertions.assertEquals("Alicia", updatedUser.getNickname());
    }

    @Test
    void testDelete() {
        UserLong user = repository.findById(3L).orElse(null);
        Assertions.assertNotNull(user);
        repository.delete(user);

        UserLong deletedUser = repository.findById(3L).orElse(null);
        Assertions.assertNull(deletedUser);
    }

    @Test
    void testDeleteById() {
        repository.deleteById(2L);

        UserLong deletedUser = repository.findById(2L).orElse(null);
        Assertions.assertNull(deletedUser);
    }

    @Test
    void testFindAllAfterDeletions() {
        repository.deleteById(1L);
        repository.deleteById(3L);

        List<UserLong> remainingUsers = repository.findAll();
        Assertions.assertNotNull(remainingUsers);
        Assertions.assertEquals(1, remainingUsers.size());
        Assertions.assertEquals(2L, remainingUsers.get(0).getId());
    }

    @Test
    void testSaveNullEntity() {
        Assertions.assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void testFindByIdNonExistent() {
        UserLong user = repository.findById(999L).orElse(null);
        Assertions.assertNull(user);
    }

    @Test
    void testQueryWithCondition() {
        Assumptions.assumeTrue(repository instanceof Queryable, "Repository does not support querying.");
        List<UserLong> admins = ((Queryable<Long, UserLong>) repository).query()
                .where("group", Operator.EQUAL, "admin")
                .all();

        Assertions.assertNotNull(admins);
        Assertions.assertEquals(1, admins.size());
        Assertions.assertEquals("Alice", admins.get(0).getName());
    }

    @Test
    void testQueryWithMultipleConditions() {
        Assumptions.assumeTrue(repository instanceof Queryable, "Repository does not support querying.");
        List<UserLong> result = ((Queryable<Long, UserLong>) repository).query()
                .where("active", Operator.EQUAL, true)
                .where("age", Operator.GREATER_THAN, 30)
                .all();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Charlie", result.get(0).getName());
    }

    @Test
    void testSaveAllAndFindAll() {
        List<UserLong> newUsers = List.of(
                new UserLong(4L, "David", 28, true, "user", null),
                new UserLong(5L, "Eve", 22, false, "user", "Evie")
        );
        repository.saveAll(newUsers);
        List<UserLong> allUsers = repository.findAll();
        Assertions.assertNotNull(allUsers);
        Assertions.assertEquals(5, allUsers.size());
        Assertions.assertTrue(allUsers.stream().anyMatch(u -> u.getId() == 4L));
        Assertions.assertTrue(allUsers.stream().anyMatch(u -> u.getId() == 5L));
    }

    @Test
    void testQueryByNullField() {
        Assumptions.assumeTrue(repository instanceof Queryable, "Repository does not support querying.");
        List<UserLong> usersWithNoNickname = ((Queryable<Long, UserLong>) repository).query()
                .where("nickname", Operator.EQUAL, null)
                .all();

        Assertions.assertNotNull(usersWithNoNickname);
        Assertions.assertEquals(1, usersWithNoNickname.size());
        Assertions.assertEquals("Bob", usersWithNoNickname.get(0).getName());
    }

    @Test
    void testQueryDelete() {
        Assumptions.assumeTrue(repository instanceof Queryable, "Repository does not support querying.");
        int deletedCount = ((Queryable<Long, UserLong>) repository).query()
                .where("active", Operator.EQUAL, false)
                .delete();
        Assertions.assertEquals(1, deletedCount);

        List<UserLong> remainingUsers = repository.findAll();
        Assertions.assertNotNull(remainingUsers);
        Assertions.assertEquals(2, remainingUsers.size());
        Assertions.assertTrue(remainingUsers.stream().noneMatch(u -> u.getName().equals("Bob")));
    }

    @Test
    void testQueryAndAndOr() {
        Assumptions.assumeTrue(repository instanceof Queryable, "Repository does not support querying.");
        List<UserLong> result = ((Queryable<Long, UserLong>) repository).query()
                .where("group", Operator.EQUAL, "admin")
                .or()
                .where("group", Operator.EQUAL, "moderator")
                .all();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(u -> u.getName().equals("Alice")));
        Assertions.assertTrue(result.stream().anyMatch(u -> u.getName().equals("Charlie")));
    }

    @Test
    public void testUpMigration() throws Exception {
        final Repository<Long, UserLongAdd> newRepo = database.getRepository(UserLongAdd.class);

        Assumptions.assumeTrue(newRepo instanceof MigrationExecutor, "Repository does not support migration.");

        ((MigrationExecutor) newRepo).migrate();

        final List<UserLongAdd> users = newRepo.findAll();
        Assertions.assertEquals(3, users.size());
        Assertions.assertTrue(users.stream().allMatch((user) -> user.getId() != null && user.getDob() == null));
        final LocalDateTime dob = LocalDateTime.of(1995, Month.APRIL, 15, 10, 30);
        Assertions.assertDoesNotThrow(() -> {
            final UserLongAdd user = new UserLongAdd(4L, "Paul", 22, true, "user", "paulo", dob);
            newRepo.save(user);
        });
        Assertions.assertEquals(4, newRepo.findAll().size());
        Assertions.assertEquals(dob, newRepo.findById(4L).map(UserLongAdd::getDob).orElse(null));
    }
}
