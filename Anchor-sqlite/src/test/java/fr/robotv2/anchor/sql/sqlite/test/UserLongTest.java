package fr.robotv2.anchor.sql.sqlite.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import fr.robotv2.anchor.test.BlobStoredFieldContract;
import fr.robotv2.anchor.test.RepositoryContractSupport;
import fr.robotv2.anchor.test.SchemaMigrationContract;

import java.nio.file.Path;

public class UserLongTest extends RepositoryContractSupport
        implements SchemaMigrationContract, BlobStoredFieldContract {

    @Override
    protected Database createDatabase(Path tempDir) {
        final Path dbFile = tempDir.resolve("test_long.db");
        return new SqliteDatabase(dbFile.toFile());
    }

    @Override
    protected void onRepositoryReady(Repository<?, ?> repository) {
        ((SQLRepository<?, ?>) repository).createTableIfNotExists();
    }
}
