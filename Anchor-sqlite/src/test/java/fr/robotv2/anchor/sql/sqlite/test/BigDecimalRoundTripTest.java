package fr.robotv2.anchor.sql.sqlite.test;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.repository.Identifiable;
import fr.robotv2.anchor.api.repository.Repository;
import fr.robotv2.anchor.sql.repository.SQLRepository;
import fr.robotv2.anchor.sql.sqlite.SqliteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigDecimalRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadsBigDecimalFieldsReturnedAsSqliteRealValues() {
        Path databaseFile = tempDir.resolve("big-decimal.db");

        SqliteDatabase writer = new SqliteDatabase(databaseFile.toFile());
        writer.connect();
        Repository<Long, DecimalEntity> writerRepository = writer.getRepository(DecimalEntity.class);
        ((SQLRepository<?, ?>) writerRepository).createTableIfNotExists();
        writerRepository.save(new DecimalEntity(1L, new BigDecimal("2.0"), new BigDecimal("10.25")));
        writer.disconnect();

        SqliteDatabase reader = new SqliteDatabase(databaseFile.toFile());
        reader.connect();
        try {
            Repository<Long, DecimalEntity> readerRepository = reader.getRepository(DecimalEntity.class);
            DecimalEntity restored = readerRepository.findById(1L).orElseThrow();

            assertEquals(new BigDecimal("2.0"), restored.getProgress());
            assertEquals(new BigDecimal("10.25"), restored.getRequired());
        } finally {
            reader.disconnect();
        }
    }

    @Entity("decimal_entities")
    public static class DecimalEntity implements Identifiable<Long> {

        @Id
        @Column("id")
        private Long id;

        @Column("progress")
        private BigDecimal progress;

        @Column("required")
        private BigDecimal required;

        public DecimalEntity() {
        }

        DecimalEntity(Long id, BigDecimal progress, BigDecimal required) {
            this.id = id;
            this.progress = progress;
            this.required = required;
        }

        @Override
        public Long getId() {
            return id;
        }

        public BigDecimal getProgress() {
            return progress;
        }

        public BigDecimal getRequired() {
            return required;
        }
    }
}
