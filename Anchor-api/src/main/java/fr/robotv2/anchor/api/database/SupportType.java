package fr.robotv2.anchor.api.database;

public enum SupportType {

    WRAPPED_ASYNC, // See AsyncRepository#wrap
    ASYNC,
    QUERY,
    TRANSACTION,
    MIGRATION,
    ;

}
