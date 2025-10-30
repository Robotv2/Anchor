package fr.robotv2.anchor.mongodb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.mongodb.MongoDBConfiguration;
import fr.robotv2.anchor.mongodb.MongoDBDatabase;
import fr.robotv2.anchor.test.AbstractAsyncUserLongTest;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Path;

public class AsyncUserLongTest extends AbstractAsyncUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        // Skip if MongoDB is not available
        try {
            MongoDBConfiguration config = new MongoDBConfiguration("localhost", 27017, "test_anchor_async", null, null);
            return new MongoDBDatabase(config);
        } catch (Exception e) {
            Assumptions.abort("MongoDB not available: " + e.getMessage());
            return null;
        }
    }
}
