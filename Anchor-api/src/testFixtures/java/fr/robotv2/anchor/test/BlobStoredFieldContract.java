package fr.robotv2.anchor.test;

import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.test.model.UserLongBlob;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public interface BlobStoredFieldContract extends CoreRepositoryContract {

    void prepareContractRepository(Repository<?, ?> repository);

    @Test
    default void roundTripsASerializableBlob() {
        Repository<UUID, UserLongBlob> blobRepository =
                contractDatabase().getRepository(UserLongBlob.class);
        prepareContractRepository(blobRepository);

        UserLongBlob blobUser = new UserLongBlob(UUID.randomUUID());
        blobRepository.save(blobUser);
        UserLongBlob retrieved = blobRepository.findById(blobUser.getId()).orElseThrow();

        assertNotNull(retrieved.getBlob());
        assertEquals(blobUser.getBlob().getValue(), retrieved.getBlob().getValue());
        assertEquals(blobUser.getBlob().getLongValue(), retrieved.getBlob().getLongValue());
        assertEquals(blobUser.getBlob().getDoubleValue(), retrieved.getBlob().getDoubleValue());
        assertEquals(blobUser.getBlob().getEnumValue(), retrieved.getBlob().getEnumValue());
        assertEquals(blobUser.getBlob().getMap(), retrieved.getBlob().getMap());
    }
}
