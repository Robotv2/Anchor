package fr.robotv2.anchor.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AsyncRepository<ID, E extends Identifiable<ID>> {

    CompletableFuture<Void> save(E entity);

    CompletableFuture<Void> saveAll(Collection<E> entities);

    CompletableFuture<Void> delete(E entity);

    CompletableFuture<Void> deleteById(ID id);

    CompletableFuture<Void> deleteAll(Collection<E> entities);

    CompletableFuture<Void> deleteAllById(Collection<ID> ids);

    CompletableFuture<Optional<E>> findById(ID id);

    CompletableFuture<List<E>> findAll();

    static <ID, E extends Identifiable<ID>> AsyncRepository<ID, E> wrap(Repository<ID, E> repository) {
        return new AsyncRepository<>() {

            @Override
            public CompletableFuture<Void> save(E entity) {
                return CompletableFuture.runAsync(() -> repository.save(entity));
            }

            @Override
            public CompletableFuture<Void> saveAll(Collection<E> entities) {
                return CompletableFuture.runAsync(() -> repository.saveAll(entities));
            }

            @Override
            public CompletableFuture<Void> delete(E entity) {
                return CompletableFuture.runAsync(() -> repository.delete(entity));
            }

            @Override
            public CompletableFuture<Void> deleteById(ID id) {
                return CompletableFuture.runAsync(() -> repository.deleteById(id));
            }

            @Override
            public CompletableFuture<Void> deleteAll(Collection<E> entities) {
                return CompletableFuture.runAsync(() -> repository.deleteAll(entities));
            }

            @Override
            public CompletableFuture<Void> deleteAllById(Collection<ID> ids) {
                return CompletableFuture.runAsync(() -> repository.deleteAllById(ids));
            }

            @Override
            public CompletableFuture<Optional<E>> findById(ID id) {
                return CompletableFuture.supplyAsync(() -> repository.findById(id));
            }

            @Override
            public CompletableFuture<List<E>> findAll() {
                return CompletableFuture.supplyAsync(repository::findAll);
            }
        };
    }
}
