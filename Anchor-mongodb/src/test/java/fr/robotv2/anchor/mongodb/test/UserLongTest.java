package fr.robotv2.anchor.mongodb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.mongodb.MongoDBConfiguration;
import fr.robotv2.anchor.mongodb.MongoDBDatabase;
import fr.robotv2.anchor.test.AbstractUserLongTest;

import java.nio.file.Path;

/**
 * Test class for MongoDB repository implementation.
 * <p>
 * This test extends AbstractUserLongTest to ensure MongoDB implementation
 * conforms to the standard repository behavior.
 * </p>
 * <p>
 * Note: These tests require a running MongoDB instance on localhost:27017.
 * Set the MONGODB_TEST_ENABLED environment variable to "true" to run these tests.
 * </p>
 */
public class UserLongTest extends AbstractUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        // Use a test database name to avoid conflicts
        MongoDBConfiguration config = new MongoDBConfiguration(
                "localhost",
                27017,
                "anchor_test_" + System.currentTimeMillis(),
                null,  // No authentication for local testing
                null
        );
        return new MongoDBDatabase(config);
    }
}
