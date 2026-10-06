package com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.enrichers;

import com.liquilabs.vankoo.gateway.domain.model.valueobjects.UserAuthentication;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

public class AuthenticationRequestEnricher {
    public static ServerHttpRequest enrichWithAuthentication(ServerHttpRequest request, UserAuthentication auth) {
        var roles = formatRoles(auth.roles());
        return request.mutate()
                .header("X-User-Id", auth.userId())
                .header("X-User-Email", auth.email())
                .header("X-User-Roles", roles)
                .build();
    }

    private static String formatRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "";
        return String.join(",", roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .toList());
    }
}