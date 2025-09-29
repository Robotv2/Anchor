package fr.robotv2.anchor.api.repository;

public interface Queryable<ID, T extends Identifiable<ID>> {

    QueryBuilder<ID, T> query();
}
