package fr.robotv2.anchor.api.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SupportTypeTest {

    @Test
    void transactionIsNotDeclaredWithoutTransactionInterface() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SupportType.valueOf("TRANSACTION"));
    }
}
