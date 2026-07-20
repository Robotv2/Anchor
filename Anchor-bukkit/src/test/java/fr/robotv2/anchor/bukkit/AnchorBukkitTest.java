package fr.robotv2.anchor.bukkit;

import fr.robotv2.anchor.json.JsonDatabase;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorBukkitTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDefaultNestedAndRootJsonFiles() {
        JsonDatabase defaultDatabase = assertInstanceOf(
                JsonDatabase.class,
                AnchorBukkit.resolveDatabase(plugin(), new MemoryConfiguration())
        );
        assertEquals(tempDir.resolve("data.json").toFile(), defaultDatabase.getFile());

        MemoryConfiguration nested = configuration("json");
        nested.createSection("json").set("file", "nested.json");
        JsonDatabase nestedDatabase = assertInstanceOf(
                JsonDatabase.class,
                AnchorBukkit.resolveDatabase(plugin(), nested)
        );
        assertEquals(tempDir.resolve("nested.json").toFile(), nestedDatabase.getFile());

        MemoryConfiguration root = configuration("json");
        root.set("file", "root.json");
        JsonDatabase rootDatabase = assertInstanceOf(
                JsonDatabase.class,
                AnchorBukkit.resolveDatabase(plugin(), root)
        );
        assertEquals(tempDir.resolve("root.json").toFile(), rootDatabase.getFile());
    }

    @Test
    void resolvesSqliteAliasesAndFileLocationsIndependentlyOfDefaultLocale() throws Exception {
        Locale previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            for(String type : List.of("SQLITE", "SQLITE3")) {
                String filename = type.toLowerCase(Locale.ROOT) + ".db";
                MemoryConfiguration configuration = configuration(type);
                if(type.equals("SQLITE")) {
                    configuration.createSection("sqlite").set("file", filename);
                } else {
                    configuration.set("file", filename);
                }

                SqliteDatabase database = assertInstanceOf(
                        SqliteDatabase.class,
                        AnchorBukkit.resolveDatabase(plugin(), configuration)
                );
                File expectedFile = tempDir.resolve(filename).toFile();
                try(Connection connection = database.getConnection()) {
                    assertEquals(
                            "jdbc:sqlite:" + expectedFile.getPath(),
                            connection.getMetaData().getURL()
                    );
                } finally {
                    database.disconnect();
                }
            }
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    void detectsAvailableModules() {
        assertAll(
                () -> assertTrue(AnchorBukkit.isJsonAvailable()),
                () -> assertTrue(AnchorBukkit.isSqliteAvailable()),
                () -> assertTrue(AnchorBukkit.isMariadbAvailable())
        );
    }

    @Test
    void rejectsMissingMariaDbConfigurationSections() {
        for(String type : List.of("mariadb", "mysql")) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> AnchorBukkit.resolveDatabase(plugin(), configuration(type))
            );
            assertEquals("MariaDB configuration section is missing.", exception.getMessage());
        }
    }

    @Test
    void rejectsUnknownDatabaseTypes() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AnchorBukkit.resolveDatabase(plugin(), configuration("redis"))
        );
        assertEquals("Unknown database type: redis", exception.getMessage());
    }

    private MemoryConfiguration configuration(String type) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("type", type);
        return configuration;
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if(method.getName().equals("getDataFolder")) {
                        return tempDir.toFile();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
