package fr.robotv2.anchor.test.model;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.repository.Identifiable;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

@Entity("users_long_blob")
public class UserLongBlob implements Identifiable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column(value = "blob", blob = true)
    private BlobClass blob;

    public UserLongBlob(UUID id) {
        this.id = id;
        this.blob = new BlobClass();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public BlobClass getBlob() {
        return blob;
    }

    public static class BlobClass implements Serializable {

        private final int value;

        private final long longValue;

        private final double doubleValue;

        public BlobClass(int value, long longValue, double doubleValue) {
            this.value = value;
            this.longValue = longValue;
            this.doubleValue = doubleValue;
        }

        public BlobClass() {
            final Random random = new Random();
            this.value = random.nextInt();
            this.longValue = random.nextLong();
            this.doubleValue = random.nextDouble();
        }

        public int getValue() {
            return value;
        }

        public long getLongValue() {
            return longValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }
    }
}
