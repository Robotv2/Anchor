package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.repository.MigrationExecutor;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.test.model.UserLongAdd;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public interface SchemaMigrationContract extends QueryCapabilityContract {

    @Test
    default void addsAStoredFieldWithoutLosingRows() throws Exception {
        Repository<Long, UserLongAdd> migratedRepository =
                contractDatabase().getRepository(UserLongAdd.class);
        MigrationExecutor migration = assertInstanceOf(MigrationExecutor.class, migratedRepository);

        migration.migrate();

        List<UserLongAdd> users = migratedRepository.findAll();
        assertEquals(3, users.size());
        assertTrue(users.stream().allMatch(user -> user.getId() != null && user.getDob() == null));

        LocalDateTime dateOfBirth = LocalDateTime.of(1995, Month.APRIL, 15, 10, 30);
        migratedRepository.save(new UserLongAdd(
                4L, "Paul", 22, true, "user", "paulo", dateOfBirth
        ));
        assertEquals(dateOfBirth, migratedRepository.findById(4L).orElseThrow().getDob());
    }
}
