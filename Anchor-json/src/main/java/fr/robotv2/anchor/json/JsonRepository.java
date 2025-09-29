package fr.robotv2.anchor.json;

import fr.robotv2.anchor.api.metadata.EntityMetadata;
import fr.robotv2.anchor.api.metadata.MetadataProcessor;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JsonRepository<ID, T extends Identifiable<ID>> implements Repository<ID, T> {

    private final JsonDatabase database;
    private final Class<T> cls;
    private final EntityMetadata metadata;

    public JsonRepository(JsonDatabase database, Class<T> cls) {
        this.database = database;
        this.cls = cls;
        this.metadata = MetadataProcessor.getMetadata(cls);
    }

    @Override
    public void save(T entity) {
        final File file = resolveFile(entity.getId());
        ensureFileExists(file);
        try(final BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            database.getGson().toJson(entity, writer);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save entity with ID: " + entity.getId(), exception);
        }
    }

    @Override
    public void saveAll(Collection<T> entities) {
        if(entities.isEmpty()) {
            return;
        }

        for(T entity : entities) {
            save(entity);
        }
    }

    @Override
    public void delete(T entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(ID id) {
        final File file = resolveFile(id);
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Failed to delete entity with ID: " + id);
        }
    }

    @Override
    public void deleteAll(Collection<T> entities) {
        if(entities.isEmpty()) {
            return;
        }

        for(T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAllById(Collection<ID> ids) {
        if(ids.isEmpty()) {
            return;
        }

        for(ID id : ids) {
            deleteById(id);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        final File file = resolveFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }

        try(Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            final T entity = database.getGson().fromJson(reader, cls);
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

        final File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return List.of();
        }

        return Stream.of(files).map(file -> {
            try(Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                return database.getGson().fromJson(reader, cls);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to read entity from file: " + file.getName(), exception);
            }
        }).toList();
    }

    public File resolveFile(ID id) {
        final String format = "%s_%s.json";
        return new File(database.getFile(),
                String.format(
                        format,
                        cls.getSimpleName().toLowerCase(),
                        id
                )
        );
    }

    private void ensureFileExists(File file) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create file: " + file.getPath(), e);
            }
        }
    }
}
