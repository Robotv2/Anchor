package fr.robotv2.anchor.json;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class JsonRepository<ID, T extends Identifiable<ID>> implements Repository<ID, T> {

    private final JsonDatabase database;
    private final Class<T> cls;

    private final String fileNameFormat;
    private final Map<ID, SoftReference<T>> cache = new ConcurrentHashMap<>();
    private volatile boolean directoryInitialized = false;

    public JsonRepository(JsonDatabase database, Class<T> cls) {
        this.database = database;
        this.cls = cls;

        EntityMetadata metadata = MetadataProcessor.getMetadata(cls);
        this.fileNameFormat = metadata.getEntityName().toLowerCase() + "_%s.json";
    }

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        final File file = resolveFile(entity.getId());
        final File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");

        ensureDirectoryExists();

        try(final BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
            database.getGson().toJson(entity, writer);
        } catch (IOException exception) {
            tempFile.delete();
            throw new RuntimeException("Failed to save entity with ID: " + entity.getId(), exception);
        }

        try {
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            cache.put(entity.getId(), new SoftReference<>(entity));
        } catch (IOException exception) {
            tempFile.delete();
            throw new RuntimeException("Failed to move temp file for entity: " + entity.getId(), exception);
        }
    }

    @Override
    public void saveAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        if(entities.isEmpty()) {
            return;
        }

        entities.forEach(this::save);
    }

    @Override
    public void delete(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        cache.remove(id);
        final File file = resolveFile(id);
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Failed to delete entity with ID: " + id);
        }
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        Objects.requireNonNull(entities, "Entities collection cannot be null");
        if(entities.isEmpty()) {
            return;
        }

        entities.forEach(this::delete);
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        Objects.requireNonNull(ids, "IDs collection cannot be null");
        if(ids.isEmpty()) {
            return;
        }

        ids.forEach(this::deleteById);
    }

    @Override
    public Optional<T> findById(ID id) {
        // Check cache first
        SoftReference<T> ref = cache.get(id);
        if (ref != null) {
            T cached = ref.get();
            if (cached != null) {
                return Optional.of(cached);
            }
            cache.remove(id);
        }

        final File file = resolveFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }

        try(Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final T entity = database.getGson().fromJson(reader, cls);
            if (entity != null) {
                cache.put(id, new SoftReference<>(entity));
            }
            return Optional.ofNullable(entity);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read entity with ID: " + id, exception);
        }
    }

    @Override
    public List<T> findAll() {
        final File dir = database.getFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(dir.toPath())) {
            return paths.filter(path -> path.toString().endsWith(".json")).map(this::readEntityFromFile).toList();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to list directory", exception);
        }
    }

    public File resolveFile(ID id) {
        return new File(database.getFile(), String.format(fileNameFormat, id));
    }

    public void clearCache() {
        cache.clear();
    }

    public void evictCache(ID id) {
        cache.remove(id);
    }

    private void ensureDirectoryExists() {
        // Double-checked locking for thread safety
        if (!directoryInitialized) {
            synchronized (this) {
                if (!directoryInitialized) {
                    database.getFile().mkdirs();
                    directoryInitialized = true;
                }
            }
        }
    }

    private T readEntityFromFile(Path path) {
        try(Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T entity = database.getGson().fromJson(reader, cls);
            if (entity != null) {
                cache.put(entity.getId(), new SoftReference<>(entity));
            }
            return entity;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read entity from file: " + path.getFileName(), exception);
        }
    }

    @Override
    public void beginTransaction() {
        throw new UnsupportedOperationException("Transactions are not supported by JSON database");
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException("Transactions are not supported by JSON database");
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException("Transactions are not supported by JSON database");
    }

    @Override
    public void executeInTransaction(Consumer<Repository<ID, T>> operations) {
        throw new UnsupportedOperationException("Transactions are not supported by JSON database");
    }
}
