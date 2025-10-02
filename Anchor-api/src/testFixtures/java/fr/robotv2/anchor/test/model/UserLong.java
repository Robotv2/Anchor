package fr.robotv2.anchor.test.model;

import fr.robotv2.anchor.api.annotation.Column;
import fr.robotv2.anchor.api.annotation.Entity;
import fr.robotv2.anchor.api.annotation.Id;
import fr.robotv2.anchor.api.annotation.Index;
import fr.robotv2.anchor.api.repository.Identifiable;

/**
 * Test fixture entity for Anchor ORM testing.
 * <p>
 * This entity is used throughout the Anchor test suite to verify ORM functionality
 * across different database implementations (SQLite, MariaDB, JSON).
 * </p>
 *
 * @since 1.0
 */
@Entity("users_long")
public class UserLong implements Identifiable<Long> {

    @Id
    @Column("id")
    private Long id;

    @Column("name")
    @Index
    private String name;

    @Column("age")
    private Integer age;

    @Column("active")
    private Boolean active;

    // Reserved word column to test quoting:
    @Column("group")
    private String groupName;

    @Column("nickname")
    private String nickname;

    public UserLong() {}

    public UserLong(Long id, String name, Integer age, Boolean active, String groupName, String nickname) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.active = active;
        this.groupName = groupName;
        this.nickname = nickname;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public Boolean getActive() {
        return active;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
