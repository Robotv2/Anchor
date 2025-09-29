package fr.robotv2.anchor.sql.database;

import fr.robotv2.anchor.api.database.Database;
import fr.robotv2.anchor.sql.dialect.SQLDialect;
import fr.robotv2.anchor.sql.mapper.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public interface SQLDatabase extends Database {

    Connection getConnection() throws SQLException;

    SQLDialect getDialect();

    boolean execute(String sql) throws SQLException;

    int executeUpdate(String sql, Collection<Object> parameters) throws SQLException;

    int executeBatchUpdate(String sql, Collection<Collection<Object>> parameters) throws SQLException;

    <R> List<R> query(String sql, Collection<Object> parameters, RowMapper<R> mapper) throws SQLException;

    <R> List<R> queryRaw(String sql, RowMapper<R> mapper) throws SQLException;
}
