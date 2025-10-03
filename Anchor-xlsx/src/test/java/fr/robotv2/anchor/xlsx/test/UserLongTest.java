package fr.robotv2.anchor.xlsx.test;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.test.AbstractUserLongTest;
import fr.robotv2.anchor.xlsx.XlsxDatabase;

import java.nio.file.Path;

public class UserLongTest extends AbstractUserLongTest {

    @Override
    protected Database createDatabase(Path tempDir) {
        final Path xlsxFile = tempDir.resolve("test_long.xlsx");
        return new XlsxDatabase(xlsxFile.toFile());
    }
}
