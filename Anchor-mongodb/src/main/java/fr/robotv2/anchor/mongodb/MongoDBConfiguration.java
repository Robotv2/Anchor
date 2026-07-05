package fr.robotv2.anchor.mongodb;

/**
 * Configuration record for MongoDB database connection.
 * <p>
 * This record holds the connection parameters required to establish
 * a connection to a MongoDB database.
 * </p>
 *
 * @param host the MongoDB server hostname
 * @param port the MongoDB server port
 * @param database the database name to connect to
 * @param username the username for authentication (can be null for no auth)
 * @param password the password for authentication (can be null for no auth)
 */
public record MongoDBConfiguration(String host, int port, String database, String username, String password) {
}
