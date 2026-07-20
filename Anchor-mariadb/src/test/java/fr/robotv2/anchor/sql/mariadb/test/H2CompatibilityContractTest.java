package fr.robotv2.anchor.sql.mariadb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.test.BlobStoredFieldContract;
import fr.robotv2.anchor.test.RepositoryContractSupport;
import fr.robotv2.anchor.test.SchemaMigrationContract;
import fr.robotv2.anchor.test.model.UserLong;

import java.nio.file.Path;

public class H2CompatibilityContractTest extends RepositoryContractSupport
        implements SchemaMigrationContract, BlobStoredFieldContract {

    @Override
    protected Database createDatabase(Path tempDir) {
        return new H2MariaDBCompatibilityDatabase();
    }

    @Override
    protected void onRepositoryReady(Repository<?, ?> repository) {
        ((SQLRepository<?, ?>) repository).createTableIfNotExists();
    }

    @Override
    protected void onTearDown(Database database, Repository<Long, UserLong> repository) {
        ((SQLRepository<?, ?>) repository).dropTable();
    }
}
