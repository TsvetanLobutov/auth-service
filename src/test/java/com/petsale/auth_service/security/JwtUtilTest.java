package com.petsale.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "mySuperSecretKeyForPetShopEcommerceApp12345";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void generateTokenCreatesSignedTokenWithSubjectAndExpiration() {
        String token = jwtUtil.generateToken("testUser");

        Claims claims = claims(token);

        assertThat(claims.getSubject()).isEqualTo("testUser");
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void generatedTokensContainDifferentValuesForDifferentUsers() {
        assertThat(jwtUtil.generateToken("alice")).isNotEqualTo(jwtUtil.generateToken("bob"));
    }

    @Test
    void tokenExpiresTenHoursAfterItIsIssued() {
        Claims claims = claims(jwtUtil.generateToken("testUser"));

        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(lifetimeMillis)
                .isBetween(TimeUnit.HOURS.toMillis(10) - 2_000, TimeUnit.HOURS.toMillis(10) + 2_000);
    }

    @Test
    void tokenIsSignedWithHs256() {
        String token = jwtUtil.generateToken("testUser");

        String algorithm = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getHeader()
                .getAlgorithm();

        assertThat(algorithm).isEqualTo("HS256");
    }

    @Test
    void tokenIsRejectedWhenVerifiedWithAnotherSecret() {
        String token = jwtUtil.generateToken("testUser");
        var anotherKey = Keys.hmacShaKeyFor(
                "aCompletelyDifferentSecretKeyThatIsLongEnough".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> Jwts.parser()
                .verifyWith(anotherKey)
                .build()
                .parseSignedClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims("this-is-not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private javax.crypto.SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
