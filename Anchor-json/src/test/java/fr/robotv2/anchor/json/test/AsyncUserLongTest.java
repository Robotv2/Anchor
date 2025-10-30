package fr.robotv2.anchor.json.test;

import fr.robotv2.anchor.json.JsonDatabase;
import fr.robotv2.anchor.test.AbstractAsyncUserLongTest;

import java.nio.file.Path;

public class AsyncUserLongTest extends AbstractAsyncUserLongTest {

    @Override
    protected JsonDatabase createDatabase(Path tempDir) {
        return new JsonDatabase(tempDir.toFile());
    }
}
