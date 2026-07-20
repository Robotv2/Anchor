package fr.robotv2.anchor.mongodb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.mongodb.MongoDBConfiguration;
import fr.robotv2.anchor.mongodb.MongoDBDatabase;
import fr.robotv2.anchor.test.QueryCapabilityContract;
import fr.robotv2.anchor.test.RepositoryContractSupport;
import fr.robotv2.anchor.test.model.UserLong;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.nio.file.Path;
import java.util.UUID;

@Testcontainers
class MongoDBContainerContractTest extends RepositoryContractSupport
        implements QueryCapabilityContract {

    @Container
    private static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:8.0");

    @Override
    protected Database createDatabase(Path temporaryDirectory) {
        return new MongoDBDatabase(new MongoDBConfiguration(
                MONGODB.getHost(),
                MONGODB.getMappedPort(27017),
                "anchor_test_" + UUID.randomUUID().toString().replace("-", ""),
                null,
                null
        ));
    }

    @Override
    protected void onTearDown(Database database, Repository<Long, UserLong> repository) {
        if (database != null && database.isConnected()) {
            ((MongoDBDatabase) database).getDatabase().drop();
        }
    }
}
