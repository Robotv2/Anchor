package fr.robotv2.anchor.api.database;

import fr.robotv2.anchor.api.repository.AsyncRepository;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;

public interface Database {

    void connect();

    void disconnect();

    boolean isConnected();

    <ID, T extends Identifiable<ID>> Repository<ID, T> getRepository(Class<T> clazz);

    default <ID, T extends Identifiable<ID>> AsyncRepository<ID, T> getAsyncRepository(Class<T> clazz) {
        return AsyncRepository.wrap(getRepository(clazz));
    }
}
