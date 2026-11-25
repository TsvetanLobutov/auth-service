package com.petsale.auth_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordEncoderBeanIsBcrypt() {
        assertThat(securityConfig.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void encodedPasswordMatchesTheRawPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("s3cret");

        assertThat(encoded).isNotEqualTo("s3cret");
        assertThat(encoder.matches("s3cret", encoded)).isTrue();
    }

    @Test
    void encodedPasswordDoesNotMatchAnotherPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder.matches("wrong-password", encoder.encode("s3cret"))).isFalse();
    }

    @Test
    void saltingProducesADifferentHashForTheSamePassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder.encode("s3cret")).isNotEqualTo(encoder.encode("s3cret"));
    }
}
