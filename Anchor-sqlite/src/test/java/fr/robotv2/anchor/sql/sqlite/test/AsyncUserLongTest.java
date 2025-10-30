package fr.robotv2.anchor.sql.sqlite.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import fr.robotv2.anchor.test.AbstractAsyncUserLongTest;

import java.nio.file.Path;

public class AsyncUserLongTest extends AbstractAsyncUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        final Path dbFile = tempDir.resolve("test_async.db");
        return new SqliteDatabase(dbFile.toFile());
    }

    @Override
    protected void onRepositoryReady(Repository<?, ?> repository) {
        ((SQLRepository<?, ?>) repository).createTableIfNotExists();
    }
}
