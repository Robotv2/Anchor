package fr.robotv2.anchor.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Repository<ID, E extends Identifiable<ID>> {

    void save(E entity);

    void saveAll(Collection<E> entities);

    void delete(E entity);

    void deleteById(ID id);

    void deleteAll(Collection<E> entities);

    void deleteAllById(Collection<ID> ids);

    Optional<E> findById(ID id);

    List<E> findAll();
}
