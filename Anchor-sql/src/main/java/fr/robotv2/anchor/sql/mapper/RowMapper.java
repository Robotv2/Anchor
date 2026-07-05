package fr.robotv2.anchor.sql.mapper;

import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<R> {
    R map(java.sql.ResultSet rs) throws SQLException;
}
