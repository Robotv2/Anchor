package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.database.SupportType;
import fr.robotv2.anchor.api.repository.MigrationExecutor;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public interface CoreRepositoryContract {

    Database contractDatabase();

    Repository<Long, UserLong> contractRepository();

    @Test
    default void createsStorageAndSelectsAllEntities() {
        List<UserLong> all = contractRepository().findAll();

        assertNotNull(all);
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(user -> user.getId() == 1L));
        assertTrue(all.stream().anyMatch(user -> user.getId() == 2L));
        assertTrue(all.stream().anyMatch(user -> user.getId() == 3L));
    }

    @Test
    default void findsEntityByIdentifier() {
        UserLong user = contractRepository().findById(2L).orElse(null);

        assertNotNull(user);
        assertEquals("Bob", user.getName());
        assertEquals(25, user.getAge());
        assertFalse(user.getActive());
        assertEquals("user", user.getGroupName());
        assertNull(user.getNickname());
    }

    @Test
    default void updatesAnExistingEntity() {
        UserLong user = contractRepository().findById(1L).orElseThrow();
        user.setAge(31);
        user.setNickname("Alicia");
        contractRepository().save(user);

        UserLong updatedUser = contractRepository().findById(1L).orElseThrow();
        assertEquals(31, updatedUser.getAge());
        assertEquals("Alicia", updatedUser.getNickname());
    }

    @Test
    default void deletesAnEntity() {
        UserLong user = contractRepository().findById(3L).orElseThrow();
        contractRepository().delete(user);

        assertTrue(contractRepository().findById(3L).isEmpty());
    }

    @Test
    default void deletesAnEntityByIdentifier() {
        contractRepository().deleteById(2L);

        assertTrue(contractRepository().findById(2L).isEmpty());
    }

    @Test
    default void findsRemainingEntitiesAfterDeletes() {
        contractRepository().deleteById(1L);
        contractRepository().deleteById(3L);

        List<UserLong> remainingUsers = contractRepository().findAll();
        assertEquals(1, remainingUsers.size());
        assertEquals(2L, remainingUsers.get(0).getId());
    }

    @Test
    default void rejectsNullEntity() {
        assertThrows(NullPointerException.class, () -> contractRepository().save(null));
    }

    @Test
    default void returnsEmptyForMissingIdentifier() {
        assertTrue(contractRepository().findById(999L).isEmpty());
    }

    @Test
    default void savesACompleteBatch() {
        contractRepository().saveAll(List.of(
                new UserLong(4L, "David", 28, true, "user", null),
                new UserLong(5L, "Eve", 22, false, "user", "Evie")
        ));

        List<UserLong> allUsers = contractRepository().findAll();
        assertEquals(5, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(user -> user.getId() == 4L));
        assertTrue(allUsers.stream().anyMatch(user -> user.getId() == 5L));
    }

    @Test
    default void capabilityFlagsMatchRepositoryInterfaces() {
        Database database = contractDatabase();
        Repository<Long, UserLong> repository = contractRepository();

        assertTrue(database.supports(SupportType.WRAPPED_ASYNC));
        assertFalse(database.supports(SupportType.ASYNC));
        assertEquals(repository instanceof QueryableRepository, database.supports(SupportType.QUERY));
        assertEquals(repository instanceof MigrationExecutor, database.supports(SupportType.MIGRATION));
    }
}
