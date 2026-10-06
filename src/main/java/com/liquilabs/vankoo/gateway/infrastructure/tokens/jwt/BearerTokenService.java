package com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt;

import com.liquilabs.vankoo.gateway.application.internal.outboundservices.tokens.TokenService;
import org.springframework.http.server.reactive.ServerHttpRequest;

public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(ServerHttpRequest request);
}