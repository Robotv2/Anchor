# Anchor Persistence Library

Anchor is a Java persistence library for storing annotated objects through repositories across multiple storage backends.

Anchor uses a capability-based backend model: every storage backend supports the core repository contract, while extra features such as Query Capability, Async Access, and Schema Migration vary by backend.

## Features

- Annotation-driven Entity mapping
- Repository access for saving, finding, and deleting Entity instances
- Multiple Storage Backends: SQLite, MariaDB, MongoDB, and JSON Storage
- Wrapped Async Access through `CompletableFuture` and an executor
- Query Capability where the selected Storage Backend supports it
- Schema Migration for SQL storage backends
- Bukkit Integration support code for plugin configuration

## Backend Capabilities

| Storage Backend | Core Repository | Wrapped Async Access | Query Capability | Schema Migration | Notes |
|-----------------|-----------------|----------------------|------------------|------------------|-------|
| SQLite | Yes | Yes | Yes | Yes | Embedded SQL storage backend. |
| MariaDB | Yes | Yes | Yes | Yes | SQL storage backend with HikariCP connection pooling. |
| MongoDB | Yes | Yes | Yes | No | Work-in-progress storage backend; tests require a local MongoDB instance. |
| JSON Storage | Yes | Yes | No | No | Lightweight storage backend for simple applications, local files, and tests. |

No native Async Access or transaction capability is currently declared. Wrapped Async Access is implemented by wrapping a synchronous Repository with an executor.

## Storage Backends

### SQLite

Embedded SQL storage backend suited to local applications, development, and tests.

### MariaDB

SQL storage backend for MariaDB deployments, using HikariCP for connection pooling.

### MongoDB

Document storage backend with the core repository contract and Query Capability. Treat it as work in progress until local test gating and storage index behavior are verified.

### JSON Storage

File-backed JSON Storage for simple applications, local files, and tests. It intentionally does not expose Query Capability or Schema Migration.

## Installation

Anchor `v0.1.0` is distributed as independent modules through [JitPack](https://jitpack.io/#Robotv2/Anchor). Replace `[module]` with the module you need.

**Gradle**

```gradle
repositories {
    mavenCentral()
    maven { url = uri('https://jitpack.io') }
}

dependencies {
    implementation 'com.github.Robotv2.Anchor:[module]:v0.1.0'
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Robotv2.Anchor</groupId>
    <artifactId>[module]</artifactId>
    <version>v0.1.0</version>
</dependency>
```

### Available Modules

Replace `[module]` with one of:

- `Anchor-sqlite` - SQLite storage backend
- `Anchor-mariadb` - MariaDB storage backend
- `Anchor-mongodb` - MongoDB storage backend
- `Anchor-json` - JSON storage backend
- `Anchor-bukkit` - Bukkit integration support

## Quick Start

### Define an Entity

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

### Use a Repository

```java
Database database = new SqliteDatabase(new File("data.db"));
database.connect();

Repository<UUID, User> userRepo = database.getRepository(User.class);

User user = new User();
user.setId(UUID.randomUUID());
user.setName("John Doe");
user.setEmail("john@example.com");

userRepo.save(user);
Optional<User> found = userRepo.findById(user.getId());

database.disconnect();
```

### Choose a Storage Backend

```java
// SQLite
Database sqlite = new SqliteDatabase(new File("data.db"));

// MariaDB
MariaDBConfiguration mariaConfig = new MariaDBConfiguration("localhost", 3306, "mydb", "user", "pass");
Database maria = new MariaDBDatabase(mariaConfig);

// MongoDB
MongoDBConfiguration mongoConfig = new MongoDBConfiguration("localhost", 27017, "mydb", "user", "pass");
Database mongo = new MongoDBDatabase(mongoConfig);

// JSON Storage
Database json = new JsonDatabase(new File("./data"));
```

### Check Backend Capability

```java
if (database.supports(SupportType.QUERY)) {
    QueryableRepository<UUID, User> queryable = (QueryableRepository<UUID, User>) database.getRepository(User.class);
    List<User> users = queryable.query()
            .where("name", Operator.EQUAL, "John Doe")
            .all();
}
```

## Core Annotations

| Annotation | Purpose |
|------------|---------|
| `@Entity("users")` | Marks a class as an Entity and names its storage target. |
| `@Id` | Marks the Identifier field. |
| `@Column("email")` | Marks a Stored Field and names it in storage. |
| `@Index` | Declares a Storage Index where the Storage Backend supports it. |

## Modules

- **Anchor-api** - Core interfaces, annotations, and metadata
- **Anchor-sql** - SQL Foundation shared by SQL storage backends
- **Anchor-sqlite** - SQLite storage backend
- **Anchor-mariadb** - MariaDB storage backend
- **Anchor-mongodb** - MongoDB storage backend
- **Anchor-json** - JSON Storage backend
- **Anchor-bukkit** - Bukkit Integration support

## Requirements

- Java 17 or newer
- Release line: `v0.1.x`

MongoDB support remains work in progress. Its integration tests require a local MongoDB instance and must be enabled explicitly with `MONGODB_TEST_ENABLED=true`.

## Support

For questions, issues, or contributions, please open an [issue](https://github.com/Robotv2/Anchor/issues) or pull request.

## License

Anchor is available under the [MIT License](LICENSE).
