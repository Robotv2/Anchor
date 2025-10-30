# Anchor ORM

A lightweight Java Object-Relational Mapping library that provides a simple, annotation-driven approach to database persistence with support for multiple database backends.

## Features

- Annotation-driven entity mapping
- Multiple database support (SQLite, MariaDB, MongoDB, JSON file storage)
- Synchronous and asynchronous repository patterns
- Built-in connection pooling with HikariCP
- Automatic schema migration
- Index management and optimization
- Type-safe query building
- Designed for easy integration with Minecraft plugins (see Anchor-bukkit)

## Supported Databases

### SQLite
Embedded, file-based database perfect for development, testing, and small applications.

### MariaDB
Production-ready relational database with full feature support and connection pooling.

### MongoDB
NoSQL document database perfect for flexible schema applications and scalable deployments.

### JSON
File-based storage using JSON serialization, ideal for simple applications and testing.

## Installation

Add JitPack repository to your build file and include only the database modules you need:

**Gradle**
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Database modules (choose one or more)
    implementation 'com.github.robotv2:Anchor-gradle:1.0-SNAPSHOT:[module]'
}
```

**Maven**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.robotv2</groupId>
    <artifactId>[module]</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Available Modules

Replace `[module]` with one of the following database implementations:

- `Anchor-sqlite` - SQLite database support
- `Anchor-mariadb` - MariaDB database support
- `Anchor-mongodb` - MongoDB database support
- `Anchor-json` - JSON file storage support

## Quick Start

### Define Entity

```java
@Entity("users")
public class User implements Identifiable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    @Index
    private String name;

    @Column("email")
    private String email;

    // Constructors, getters, setters...
}
```

### Database Usage

```java
// SQLite
Database database = new SqliteDatabase(new File("data.db"));
database.connect();

// MariaDB
MariaDBConfiguration config = new MariaDBConfiguration("localhost", 3306, "mydb", "user", "pass");
Database database = new MariaDBDatabase(config);
database.connect();

// MongoDB
MongoDBConfiguration mongoConfig = new MongoDBConfiguration("localhost", 27017, "mydb", "user", "pass");
Database database = new MongoDBDatabase(mongoConfig);
database.connect();

// JSON
Database database = new JsonDatabase(new File("./data"));
database.connect();

// Use repository
Repository<UUID, User> userRepo = database.getRepository(User.class);

User user = new User();
user.setId(UUID.randomUUID());
user.setName("John Doe");
user.setEmail("john@example.com");

userRepo.save(user);
Optional<User> found = userRepo.findById(user.getId());

database.disconnect();
```

## Core Annotations

| Annotation | Purpose |
|------------|---------|
| `@Entity("table_name")` | Marks a class as a database entity |
| `@Id` | Designates the primary key field |
| `@Column("column_name")` | Maps a field to a database column |
| `@Index` | Creates a database index for performance |

## Architecture

- **Anchor-api** - Core interfaces and annotations
- **Anchor-sql** - SQL database abstraction layer
- **Anchor-sqlite** - SQLite-specific implementation
- **Anchor-mariadb** - MariaDB-specific implementation
- **Anchor-mongodb** - MongoDB-specific implementation
- **Anchor-json** - JSON file storage implementation
- **Anchor-bukkit** - Minecraft plugin integration

## Requirements

- **Java 17+**

# Support 

For questions, issues, or contributions, please open an issue or pull request. 