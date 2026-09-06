package com.roost.auth;

import com.roost.config.RoostSecurityProperties;
import com.roost.user.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed access tokens. The token is a self-contained HS256 JWT whose
 * subject is the user id, carrying the username and role as claims so the
 * resource-server filter can authenticate and authorize without a DB hit.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final RoostSecurityProperties properties;

    public TokenService(JwtEncoder encoder, RoostSecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwtTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("roost")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    /** A signed token and the instant it expires. */
    public record IssuedToken(String value, Instant expiresAt) {
    }
}
