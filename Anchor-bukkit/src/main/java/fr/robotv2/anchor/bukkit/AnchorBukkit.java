package fr.robotv2.anchor.bukkit;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.json.JsonDatabase;
import fr.robotv2.anchor.sql.mariadb.MariaDBConfiguration;
import fr.robotv2.anchor.sql.mariadb.MariaDBDatabase;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class AnchorBukkit {

    public static final String JSON_PKG = "fr.robotv2.anchor.json.JsonDatabase";
    public static final String SQLITE_PKG = "fr.robotv2.anchor.sql.sqlite.SqliteDatabase";
    public static final String MARIADB_PKG = "fr.robotv2.anchor.sql.mariadb.MariaDBDatabase";

    /**
     * Downloads and loads the necessary dependencies for Anchor based on the detected classes.
     *
     * @param plugin            The Bukkit plugin instance.
     * @param directory         The directory where the libraries will be downloaded.
     */
    public static void downloadDependencies(Plugin plugin, String directory) {
        final BukkitLibraryManager manager = new BukkitLibraryManager(plugin, directory);
        manager.addMavenCentral();
        manager.addJitPack();

        if(isJsonAvailable()) {
            loadJson(manager);
        }

        if(isSqliteAvailable()) {
            loadSqlite(manager);
        }

        if(isMariadbAvailable()) {
            loadMariadb(manager);
        }
    }

    /**
     * Downloads and loads the necessary dependencies for Anchor based on the detected classes.
     *
     * @param plugin            The Bukkit plugin instance.
     * @param directory         The directory where the libraries will be downloaded.
     */
    public static void downloadDependencies(Plugin plugin, File directory) {
        downloadDependencies(plugin, directory.getAbsolutePath());
    }

    /**
     * Checks if the JSON database class is available.
     * @return
     */
    public static boolean isJsonAvailable() {
        return classExists(JSON_PKG);
    }

    /**
     * Checks if the SQLite database class is available.
     * @return
     */
    public static boolean isSqliteAvailable() {
        return classExists(SQLITE_PKG);
    }

    /**
     * Checks if the MariaDB database class is available.
     * @return
     */
    public static boolean isMariadbAvailable() {
        return classExists(MARIADB_PKG);
    }

    /**
     * Resolves a Database instance based on the provided configuration section.
     *
     * Supported types:
     * - json: Uses JsonDatabase. Configuration options: file (default: data.json)
     * - sqlite/sqlite3: Uses SqliteDatabase. Configuration options: file (default: data.database)
     * - mariadb/mysql: Uses MariaDBDatabase. Configuration options: host (default: localhost), port (default: 3306),
     *   database (default: anchor), username (default: root), password (default: empty)
     *
     * @param plugin The Bukkit plugin instance.
     * @param section The configuration section containing database settings.
     * @return A Database instance based on the configuration.
     * @throws IllegalArgumentException If the database type is unknown or required classes are missing.
     */
    public static Database resolveDatabase(Plugin plugin, ConfigurationSection section) {
        final String type = section.getString("type", "json").toLowerCase();
        return switch (type) {

            case "json" -> {
                if(!isJsonAvailable()) {
                    throw new IllegalStateException("JsonDatabase class not found. Please include the JSON module.");
                }
                String filename = "data.json";
                final ConfigurationSection dbSection = section.getConfigurationSection("json");
                if(dbSection != null) {
                    filename = section.getString("file", "data.json");
                }
                yield new JsonDatabase(new File(plugin.getDataFolder(), filename));
            }

            case "sqlite", "sqlite3" -> {
                if(!isSqliteAvailable()) {
                    throw new IllegalStateException("SqliteDatabase class not found. Please include the SQLite module.");
                }
                String filename = "data.database";
                final ConfigurationSection dbSection = section.getConfigurationSection("sqlite");
                if(dbSection != null) {
                    filename = section.getString("file", "data.database");
                }

                yield new SqliteDatabase(new File(plugin.getDataFolder(), filename));
            }

            case "mariadb", "mysql" -> {
                if(!isMariadbAvailable()) {
                    throw new IllegalStateException("MariaDBDatabase class not found. Please include the MariaDB module.");
                }

                final ConfigurationSection dbSection;

                if(section.isConfigurationSection("mariadb")) {
                    dbSection = section.getConfigurationSection("mariadb");
                } else if(section.isConfigurationSection("mysql")) {
                    dbSection = section.getConfigurationSection("mysql");
                } else {
                    throw new IllegalArgumentException("MariaDB configuration section is missing.");
                }

                if(dbSection == null) {
                    throw new IllegalArgumentException("MariaDB configuration section is missing.");
                }

                final String host = dbSection.getString("host", "localhost");
                final int port = dbSection.getInt("port", 3306);
                final String database = dbSection.getString("database", "anchor");
                final String username = dbSection.getString("username", "root");
                final String password = dbSection.getString("password", "");
                final MariaDBConfiguration configuration = new MariaDBConfiguration(host, port, database, username, password);
                yield new MariaDBDatabase(configuration);
            }

            default -> throw new IllegalArgumentException("Unknown database type: " + type);
        };
    }

    private static void loadJson(LibraryManager manager) {
        if(classExists("com.google.gson.Gson")) {
            return; // Gson already exists
        }

        final Library gson = Library.builder()
                .groupId("com{}google{}code{}gson")
                .artifactId("gson")
                .version("2.10.1")
                .build();
        manager.loadLibrary(gson);
    }

    private static void loadSqlite(LibraryManager manager) {
        if(classExists("org.sqlite.JDBC")) {
            return; // SQLite JDBC already exists
        }
        final Library sqlite = Library.builder()
                .groupId("org{}xerial")
                .artifactId("sqlite-jdbc")
                .version("3.46.1.0")
                .build();
        manager.loadLibrary(sqlite);
    }

    private static void loadMariadb(LibraryManager manager) {
        if(classExists("org.mariadb.jdbc.Driver") || classExists("com.mysql.cj.jdbc.Driver")) {
            return; // MariaDB or MySQL JDBC already exists
        }

        final Library mariadb = Library.builder()
                .groupId("org{}mariadb{}jdbc")
                .artifactId("mariadb-java-client")
                .version("3.2.0")
                .build();
        manager.loadLibrary(mariadb);
        final Library connector = Library.builder()
                .groupId("com{}mysql")
                .artifactId("mysql-connector-j")
                .version("8.1.0")
                .build();
        manager.loadLibrary(connector);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
