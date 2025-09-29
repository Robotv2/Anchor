package fr.robotv2.anchor.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.json.serializer.LocalDataTimeAdapter;

import java.io.File;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JsonDatabase implements Database {

    public static final Supplier<Gson> DEFAULT_GSON = () -> new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .registerTypeAdapter(LocalDateTime.class, new LocalDataTimeAdapter())
            .setFieldNamingStrategy((field) -> field.getAnnotation(Column.class) != null ? field.getAnnotation(Column.class).value() : field.getName())
            .create();

    private final Map<Class<?>, JsonRepository<?, ?>> repositories = new ConcurrentHashMap<>();
    private final Gson gson;
    private final File file; // directory where JSON data is stored

    public JsonDatabase(Gson gson, File file) {
        this.gson = gson;
        this.file = file;
    }

    public JsonDatabase(File file) {
        this(DEFAULT_GSON.get(), file);
    }

    public static JsonDatabase withAdapters(GsonBuilder builder, File file) {
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDataTimeAdapter());
        return new JsonDatabase(builder.create(), file);
    }

    @Override
    public void connect() {
        // No action needed for JSON-based database
    }

    @Override
    public void disconnect() {
        // No action needed for JSON-based database
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> JsonRepository<ID, T> getRepository(Class<T> clazz) {
        return (JsonRepository<ID, T>) repositories.computeIfAbsent(clazz, c -> new JsonRepository<>(this, clazz));
    }

    public Gson getGson() {
        return gson;
    }

    public File getFile() {
        return file;
    }

}
