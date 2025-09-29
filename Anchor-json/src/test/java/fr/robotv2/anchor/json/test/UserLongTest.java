package fr.robotv2.anchor.json.test;

import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.json.JsonDatabase;
import fr.robotv2.anchor.test.AbstractUserLongTest;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserLongTest extends AbstractUserLongTest {

    @Override
    protected JsonDatabase createDatabase(Path tempDir) {
        return new JsonDatabase(tempDir.toFile());
    }

    @Override
    protected void onRepositoryReady(Repository<Long, UserLong> repository) {
        // Nothing to do
    }

    @Test
    public void testNamingStrategy() {
        Assumptions.assumeTrue(database instanceof JsonDatabase, "Database is not a JsonDatabase");
        final JsonDatabase jsonDatabase = (JsonDatabase) database;
        final Repository<Long, UserLong> repository = jsonDatabase.getRepository(UserLong.class);
        final UserLong user = repository.findById(1L).orElse(null);
        assertNotNull(user);
        assertTrue(jsonDatabase.getGson().toJson(user).contains("\"group\": \"admin\"") , "Field 'groupName' should be serialized as 'group' in JSON");
    }
}
