package com.roost.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Password hashing and JWT signing beans.
 *
 * <p>Passwords use a {@link DelegatingPasswordEncoder} that <em>encodes</em> with
 * Argon2id and can still <em>verify</em> bcrypt hashes: each stored hash is
 * prefixed with its algorithm id ({@code {argon2}}, {@code {bcrypt}}), so the
 * scheme can evolve without a migration. Argon2 is memory-hard, which blunts
 * GPU/ASIC cracking of a leaked hash.
 *
 * <p>Tokens are symmetric HS256, signed with {@code JWT_SECRET}. The same secret
 * key backs the encoder (issuing) and the decoder (the resource-server filter).
 */
@Configuration
public class CryptoConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        return new DelegatingPasswordEncoder("argon2", encoders);
    }

    @Bean
    JwtDecoder jwtDecoder(RoostSecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RoostSecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey(properties)));
    }

    private static SecretKeySpec secretKey(RoostSecurityProperties properties) {
        return new SecretKeySpec(
            properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
