package fr.robotv2.anchor.bukkit;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class AnchorBukkit {

    public static final String JSON_PKG = "fr.robotv2.anchor.json.JsonDatabase";
    public static final String SQLITE_PKG = "fr.robotv2.anchor.sql.sqlite.SqliteDatabase";
    public static final String MARIADB_PKG = "fr.robotv2.anchor.sql.mariadb.MariaDBDatabase";

    public static void downloadDependencies(Plugin plugin, String directory, String relocationPrefix) {
        final BukkitLibraryManager manager = new BukkitLibraryManager(plugin, directory);
        manager.addMavenCentral();
        manager.addJitPack();

        relocationPrefix = relocationPrefix.replace(".", "{}");

        if(classExists(JSON_PKG)) {
            loadJson(manager, relocationPrefix);
        }

        if(classExists(SQLITE_PKG)) {
            loadSqlite(manager, relocationPrefix);
        }

        if(classExists(MARIADB_PKG)) {
            loadMariadb(manager, relocationPrefix);
        }
    }

    public static void downloadDependencies(Plugin plugin, File directory, String relocationPrefix) {
        downloadDependencies(plugin, directory.getAbsolutePath(), relocationPrefix);
    }

    private static void loadJson(LibraryManager manager, String relocationPrefix) {
        if(classExists("com.google.gson.Gson")) {
            return; // Gson already exists
        }

        final Library gson = Library.builder()
                .groupId("com{}google{}code{}gson")
                .artifactId("gson")
                .version("2.10.1")
                .relocate("com{}google{}gson", relocationPrefix + "{}anchor{}gson")
                .build();
        manager.loadLibrary(gson);
    }

    private static void loadSqlite(LibraryManager manager, String relocationPrefix) {
        final Library sqlite = Library.builder()
                .groupId("org{}xerial")
                .artifactId("sqlite-jdbc")
                .version("3.46.1.0")
                .relocate("org{}sqlite", relocationPrefix + "{}anchor{}sqlite")
                .build();
        manager.loadLibrary(sqlite);
    }

    private static void loadMariadb(LibraryManager manager, String relocationPrefix) {
        final Library mariadb = Library.builder()
                .groupId("org{}mariadb{}jdbc")
                .artifactId("mariadb-java-client")
                .version("3.2.0")
                .relocate("org{}mariadb{}jdbc", relocationPrefix + "{}anchor{}mariadb{}jdbc")
                .build();
        manager.loadLibrary(mariadb);
        final Library connector = Library.builder()
                .groupId("com{}mysql")
                .artifactId("mysql-connector-j")
                .version("8.1.0")
                .relocate("com{}mysql", relocationPrefix + "{}anchor{}mysql{}mysql-connector")
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
