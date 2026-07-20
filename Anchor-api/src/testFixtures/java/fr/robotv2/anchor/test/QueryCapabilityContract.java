package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.repository.Operator;
import fr.robotv2.anchor.api.repository.QueryableRepository;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public interface QueryCapabilityContract extends CoreRepositoryContract {

    @SuppressWarnings("unchecked")
    default QueryableRepository<Long, UserLong> queryableRepository() {
        assertInstanceOf(QueryableRepository.class, contractRepository());
        return (QueryableRepository<Long, UserLong>) contractRepository();
    }

    @Test
    default void queriesWithOneCondition() {
        List<UserLong> admins = queryableRepository().query()
                .where("group", Operator.EQUAL, "admin")
                .all();

        assertEquals(1, admins.size());
        assertEquals("Alice", admins.get(0).getName());
    }

    @Test
    default void queriesWithMultipleConditions() {
        List<UserLong> result = queryableRepository().query()
                .where("active", Operator.EQUAL, true)
                .where("age", Operator.GREATER_THAN, 30)
                .all();

        assertEquals(1, result.size());
        assertEquals("Charlie", result.get(0).getName());
    }

    @Test
    default void queriesNullStoredField() {
        List<UserLong> usersWithNoNickname = queryableRepository().query()
                .where("nickname", Operator.EQUAL, null)
                .all();

        assertEquals(1, usersWithNoNickname.size());
        assertEquals("Bob", usersWithNoNickname.get(0).getName());
    }

    @Test
    default void deletesByQuery() {
        int deletedCount = queryableRepository().query()
                .where("active", Operator.EQUAL, false)
                .delete();

        assertEquals(1, deletedCount);
        assertEquals(2, contractRepository().findAll().size());
        assertTrue(contractRepository().findAll().stream().noneMatch(user -> user.getName().equals("Bob")));
    }

    @Test
    default void combinesConditionsWithOr() {
        List<UserLong> result = queryableRepository().query()
                .where("group", Operator.EQUAL, "admin")
                .or()
                .where("group", Operator.EQUAL, "moderator")
                .all();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(user -> user.getName().equals("Alice")));
        assertTrue(result.stream().anyMatch(user -> user.getName().equals("Charlie")));
    }

    @Test
    default void limitsQueryResults() {
        assertEquals(2, queryableRepository().query().limit(2).all().size());
    }
}
