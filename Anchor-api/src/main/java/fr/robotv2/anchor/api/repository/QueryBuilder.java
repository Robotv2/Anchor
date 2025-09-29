package fr.robotv2.anchor.api.repository;

import java.util.List;

public interface QueryBuilder<ID, T extends Identifiable<ID>> {

    QueryBuilder<ID, T> where(String column, Operator operator, Object value);

    QueryBuilder<ID, T> and();

    QueryBuilder<ID, T> or();

    QueryBuilder<ID, T> limit(int count);

    List<T> all();

    T one();

    int delete();
}
