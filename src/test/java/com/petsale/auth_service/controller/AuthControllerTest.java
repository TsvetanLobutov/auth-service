package com.petsale.auth_service.controller;

import com.petsale.auth_service.entity.User;
import com.petsale.auth_service.repository.UserRepository;
import com.petsale.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerCreatesEncodedUserWithUserRole() {
        AuthRequest request = request("alice", "plain-password");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        ResponseEntity<String> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("User registered successfully");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void registerRejectsExistingUsername() {
        AuthRequest request = request("alice", "password");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        ResponseEntity<String> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Username already exists");
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginReturnsJwtWhenPasswordMatches() {
        AuthRequest request = request("alice", "password");
        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded-password");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        ResponseEntity<String> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("jwt-token");
    }

    @Test
    void loginReturnsUnauthorizedWhenPasswordDoesNotMatch() {
        AuthRequest request = request("alice", "wrong-password");
        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded-password");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        ResponseEntity<String> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid credentials");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginThrowsWhenUserDoesNotExist() {
        AuthRequest request = request("missing", "password");
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> controller.login(request)
        );
    }

    private AuthRequest request(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
