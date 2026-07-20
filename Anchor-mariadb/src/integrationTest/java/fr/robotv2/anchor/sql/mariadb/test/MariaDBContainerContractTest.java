package fr.robotv2.anchor.sql.mariadb.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.mariadb.MariaDBConfiguration;
import fr.robotv2.anchor.sql.mariadb.MariaDBDatabase;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.test.BlobStoredFieldContract;
import fr.robotv2.anchor.test.RepositoryContractSupport;
import fr.robotv2.anchor.test.SchemaMigrationContract;
import fr.robotv2.anchor.test.model.UserLong;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import java.nio.file.Path;

@Testcontainers
class MariaDBContainerContractTest extends RepositoryContractSupport
        implements SchemaMigrationContract, BlobStoredFieldContract {

    @Container
    private static final MariaDBContainer MARIADB =
            new MariaDBContainer("mariadb:11.4")
                    .withDatabaseName("anchor")
                    .withUsername("anchor")
                    .withPassword("anchor");

    @Override
    protected Database createDatabase(Path temporaryDirectory) {
        return new MariaDBDatabase(new MariaDBConfiguration(
                MARIADB.getHost(),
                MARIADB.getMappedPort(3306),
                MARIADB.getDatabaseName(),
                MARIADB.getUsername(),
                MARIADB.getPassword()
        ));
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
