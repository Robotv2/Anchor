package fr.robotv2.anchor.xlsx;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.api.repository.Identifiable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XlsxDatabase implements Database {

    private final File file;
    private final Map<Class<?>, XlsxRepository<?, ?>> repositories = new ConcurrentHashMap<>();

    public XlsxDatabase(File file) {
        this.file = file;
    }

    @Override
    public void connect() {
        // No action needed for file-based database
    }

    @Override
    public void disconnect() {
        // No action needed for file-based database
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID, T extends Identifiable<ID>> XlsxRepository<ID, T> getRepository(Class<T> clazz) {
        return (XlsxRepository<ID, T>) repositories.computeIfAbsent(clazz, c -> new XlsxRepository<>(this, clazz));
    }

    @NotNull
    public File getFile() {
        return file;
    }
}
