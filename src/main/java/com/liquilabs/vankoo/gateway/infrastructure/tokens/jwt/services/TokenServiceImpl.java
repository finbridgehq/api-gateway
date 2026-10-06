package com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.services;

import com.liquilabs.vankoo.gateway.domain.model.valueobjects.UserAuthentication;
import com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class TokenServiceImpl implements BearerTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN_INDEX = 7;

    @Value("${authorization.jwt.secret}")
    private String secret;

    @Override
    public Optional<UserAuthentication> extractAuthentication(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<?> rolesRaw = claims.get("roles", List.class);
            List<String> roles = rolesRaw != null ?
                    rolesRaw.stream().map(Object::toString).toList() : List.of();

            return Optional.of(new UserAuthentication(
                    claims.getSubject(),
                    claims.get("email", String.class),
                    roles
            ));
        } catch (Exception e) {
            LOGGER.error("Could not extract authentication: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            LOGGER.error("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String getBearerTokenFrom(ServerHttpRequest request) {
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_BEGIN_INDEX);
        }
        return null;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}