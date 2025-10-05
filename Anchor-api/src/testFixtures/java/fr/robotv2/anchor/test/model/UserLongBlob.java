package fr.robotv2.anchor.test.model;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.repository.Identifiable;

import java.io.Serializable;
import java.util.Map;
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

    public UserLongBlob() {
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

        private final BlobEnum enumValue;

        private final Map<Integer, BlobEnum> map = Map.of(
                1, BlobEnum.ONE,
                2, BlobEnum.TWO,
                3, BlobEnum.THREE,
                4, BlobEnum.FOUR,
                5, BlobEnum.FIVE
        );

        public BlobClass(int value, long longValue, double doubleValue, BlobEnum enumValue) {
            this.value = value;
            this.longValue = longValue;
            this.doubleValue = doubleValue;
            this.enumValue = enumValue;
        }

        public BlobClass() {
            final Random random = new Random();
            this.value = random.nextInt();
            this.longValue = random.nextLong();
            this.doubleValue = random.nextDouble();
            this.enumValue = BlobEnum.random(random);
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

        public BlobEnum getEnumValue() {
            return enumValue;
        }

        public Map<Integer, BlobEnum> getMap() {
            return map;
        }
    }

    public enum BlobEnum {

        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        ;

        public static BlobEnum random(Random random) {
            final BlobEnum[] values = values();
            return values[random.nextInt(values.length)];
        }
    }
}
