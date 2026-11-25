package com.petsale.auth_service.repository;

import com.petsale.auth_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserAndAssignsGeneratedId() {
        User saved = userRepository.save(user("alice", "encoded-password", "ROLE_USER"));

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId()))
                .get()
                .extracting(User::getUsername)
                .isEqualTo("alice");
    }

    @Test
    void findByUsernameReturnsTheMatchingUser() {
        userRepository.save(user("alice", "alice-password", "ROLE_USER"));
        userRepository.save(user("bob", "bob-password", "ROLE_ADMIN"));

        assertThat(userRepository.findByUsername("bob"))
                .get()
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo("bob");
                    assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
                });
    }

    @Test
    void findByUsernameReturnsEmptyWhenUserDoesNotExist() {
        userRepository.save(user("alice", "encoded-password", "ROLE_USER"));

        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void rejectsTwoUsersWithTheSameUsername() {
        userRepository.saveAndFlush(user("alice", "first-password", "ROLE_USER"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(user("alice", "second-password", "ROLE_USER"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletesUser() {
        User saved = userRepository.save(user("alice", "encoded-password", "ROLE_USER"));

        userRepository.delete(saved);

        assertThat(userRepository.findByUsername("alice")).isEmpty();
    }

    private User user(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
