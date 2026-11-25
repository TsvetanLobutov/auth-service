package com.petsale.auth_service;

import com.petsale.auth_service.entity.User;
import com.petsale.auth_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the register/login flow: HTTP layer, security configuration,
 * real password hashing and the H2-backed repository working together.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-flow-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerPersistsUserWithHashedPasswordAndUserRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "s3cret")))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        User stored = userRepository.findByUsername("alice").orElseThrow();
        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getPassword()).isNotEqualTo("s3cret");
        assertThat(stored.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("s3cret", stored.getPassword())).isTrue();
        assertThat(stored.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        register("alice", "s3cret");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "another-password")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(passwordEncoder.matches("s3cret", userRepository.findByUsername("alice").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void loginReturnsJwtWhoseSubjectIsTheUsername() throws Exception {
        register("alice", "s3cret");

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "s3cret")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        register("alice", "s3cret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void registerIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("bob", "s3cret")))
                .andExpect(status().isOk());

        assertThat(userRepository.findByUsername("bob")).isPresent();
    }

    @Test
    void endpointsOutsideTheAuthNamespaceRequireAuthentication() throws Exception {
        // SecurityConfig declares no login mechanism (no httpBasic/formLogin) and no custom
        // entry point, so Spring Security falls back to Http403ForbiddenEntryPoint: an
        // unauthenticated request is answered with 403 (not 401) and never reaches the controller.
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk());
    }

    private String credentials(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }
}
