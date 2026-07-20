package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.test.model.UserLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public abstract class RepositoryContractSupport {

    @TempDir
    protected Path tempDir;

    protected Database database;
    protected Repository<Long, UserLong> repository;

    protected abstract Database createDatabase(Path temporaryDirectory);

    protected void onRepositoryReady(Repository<?, ?> readyRepository) {
    }

    protected void onTearDown(Database currentDatabase, Repository<Long, UserLong> currentRepository) {
    }

    @BeforeEach
    protected void setUpRepositoryContract() {
        database = createDatabase(tempDir);
        database.connect();
        repository = database.getRepository(UserLong.class);
        onRepositoryReady(repository);
        repository.save(new UserLong(1L, "Alice", 30, true, "admin", "Ally"));
        repository.save(new UserLong(2L, "Bob", 25, false, "user", null));
        repository.save(new UserLong(3L, "Charlie", 35, true, "moderator", "Chuck"));
    }

    @AfterEach
    protected void tearDownRepositoryContract() {
        onTearDown(database, repository);
        if (database != null) {
            database.disconnect();
        }
    }

    public final Database contractDatabase() {
        return database;
    }

    public final Repository<Long, UserLong> contractRepository() {
        return repository;
    }

    public final void prepareContractRepository(Repository<?, ?> readyRepository) {
        onRepositoryReady(readyRepository);
    }
}
