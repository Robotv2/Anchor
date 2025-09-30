package fr.robotv2.anchor.json.test;

import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.json.JsonDatabase;
import fr.robotv2.anchor.json.JsonRepository;
import fr.robotv2.anchor.test.AbstractUserLongTest;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testFileCreation() {
        Assumptions.assumeTrue(database instanceof JsonDatabase, "Database is not a JsonDatabase");
        final JsonDatabase jsonDatabase = (JsonDatabase) database;
        final JsonRepository<Long, UserLong> repository = jsonDatabase.getRepository(UserLong.class);
        final File file = repository.resolveFile(1L);
        assertTrue(file.exists(), "File for entity with ID 1 should exist");
        assertTrue(file.length() > 0, "File for entity with ID 1 should not be empty");
        assertEquals("users_long_1.json", file.getName(), "File name should match the expected pattern");
    }
}
