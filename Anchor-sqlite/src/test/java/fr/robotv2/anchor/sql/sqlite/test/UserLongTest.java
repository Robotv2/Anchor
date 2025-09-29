package fr.robotv2.anchor.sql.sqlite.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import fr.robotv2.anchor.test.AbstractUserLongTest;
import fr.robotv2.anchor.test.model.UserLong;

import java.nio.file.Path;

public class UserLongTest extends AbstractUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        final Path dbFile = tempDir.resolve("test_long.db");
        return new SqliteDatabase(dbFile.toFile());
    }

    @Override
    protected void onRepositoryReady(Repository<Long, UserLong> repository) {
        ((SQLRepository<?, ?>) repository).createTableIfNotExists();
    }
}
