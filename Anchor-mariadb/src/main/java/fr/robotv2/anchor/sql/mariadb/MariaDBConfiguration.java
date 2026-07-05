package fr.robotv2.anchor.sql.mariadb;

public record MariaDBConfiguration(String host, int port, String database, String username, String password) {
}
