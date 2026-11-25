package com.petsale.auth_service.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void storesAllFieldsThroughAccessors() {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        user.setPassword("$2a$10$encoded");
        user.setRole("ROLE_ADMIN");

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void newUserHasNoIdUntilPersisted() {
        User user = new User();

        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getRole()).isNull();
    }

    @Test
    void usersWithSameFieldValuesAreEqual() {
        User first = user(1L, "alice", "encoded", "ROLE_USER");
        User second = user(1L, "alice", "encoded", "ROLE_USER");
        User different = user(2L, "alice", "encoded", "ROLE_USER");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    private User user(Long id, String username, String password, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
