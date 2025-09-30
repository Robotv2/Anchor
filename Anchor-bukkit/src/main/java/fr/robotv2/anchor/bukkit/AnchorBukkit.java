package fr.robotv2.anchor.bukkit;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import org.bukkit.plugin.Plugin;

public class AnchorBukkit {

    public static final String JSON_PKG = "fr.robotv2.anchor.json.JsonDatabase";
    public static final String SQLITE_PKG = "fr.robotv2.anchor.sql.sqlite.SqliteDatabase";

    public static void init(Plugin plugin, String directory, String relocationPrefix) {
        final BukkitLibraryManager manager = new BukkitLibraryManager(plugin, directory);
        manager.addMavenCentral();
        manager.addJitPack();

        if(classExists(JSON_PKG)) {
            loadJson(manager, relocationPrefix); // Gson is already in spigot-api
        }

        if(classExists(SQLITE_PKG)) {
            loadHikari(manager, relocationPrefix);
            loadSqlite(manager, relocationPrefix);
        }
    }

    private static void loadJson(LibraryManager manager, String relocationPrefix) {
        final Library gson = Library.builder()
                .groupId("com{}google{}code{}gson")
                .artifactId("gson")
                .version("2.10.1")
                .build();
        manager.loadLibrary(gson);
    }

    private static void loadHikari(LibraryManager manager, String relocationPrefix) {
        final Library hikari = Library.builder()
                .groupId("com{}zaxxer")
                .artifactId("HikariCP")
                .version("7.0.2")
                .relocate("com{}zaxxer", relocationPrefix + "{}anchor{}hikari")
                .build();
        manager.loadLibrary(hikari);
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

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
