package fr.robotv2.anchor.sql.mariadb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.mariadb.InMemoryMariaDBDatabase;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.test.AbstractUserLongTest;
import fr.robotv2.anchor.test.model.UserLong;

import java.nio.file.Path;

public class UserLongTest extends AbstractUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        return new InMemoryMariaDBDatabase();
    }

    @Override
    protected void onRepositoryReady(Repository<?, ?> repository) {
        ((SQLRepository<?, ?>) repository).createTableIfNotExists();
    }

    @Override
    public void onTearDown(Database database, Repository<Long, UserLong> repository) {
        ((SQLRepository<?, ?>) repository).dropTable(); // Clean up the table after tests
    }
}
