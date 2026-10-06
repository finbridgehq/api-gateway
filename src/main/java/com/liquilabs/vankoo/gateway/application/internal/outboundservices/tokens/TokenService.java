package com.liquilabs.vankoo.gateway.application.internal.outboundservices.tokens;

import com.liquilabs.vankoo.gateway.domain.model.valueobjects.UserAuthentication;

import java.util.Optional;

public interface TokenService {
    Optional<UserAuthentication> extractAuthentication(String token);

    boolean validateToken(String token);
}
