package com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.pipeline;

import com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.BearerTokenService;
import com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.enrichers.AuthenticationRequestEnricher;
import com.liquilabs.vankoo.gateway.infrastructure.problems.GatewayProblem;
import com.liquilabs.vankoo.gateway.infrastructure.problems.ProblemDetailWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BearerAuthorizationRequestGatewayFilterFactory extends AbstractGatewayFilterFactory<BearerAuthorizationRequestGatewayFilterFactory.Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerAuthorizationRequestGatewayFilterFactory.class);
    private final BearerTokenService tokenService;
    private final ProblemDetailWriter problemDetailWriter;

    public BearerAuthorizationRequestGatewayFilterFactory(BearerTokenService tokenService,
                                                          ProblemDetailWriter problemDetailWriter) {
        super(Config.class);
        this.tokenService = tokenService;
        this.problemDetailWriter = problemDetailWriter;
    }

    public static class Config {
        // Configuration parameters could be added here if needed in the YAML
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = tokenService.getBearerTokenFrom(exchange.getRequest());
            if (token == null) {
                LOGGER.warn("Missing or invalid Authorization header");
                return unauthenticated(exchange);
            }
            if (!tokenService.validateToken(token)) {
                LOGGER.error("Token validation failed");
                return unauthenticated(exchange);
            }
            return tokenService.extractAuthentication(token)
                    .map(auth -> {
                        // Log de inyección para saber que to' va bien
                        LOGGER.info("Successfully authenticated user: {} - Injecting identity headers", auth.userId());
                        var enrichedRequest = AuthenticationRequestEnricher
                                .enrichWithAuthentication(exchange.getRequest(), auth);
                        return chain.filter(exchange.mutate().request(enrichedRequest).build());
                    })
                    .orElseGet(() -> {
                        LOGGER.error("Failed to extract claims from token");
                        return unauthenticated(exchange);
                    });
        };
    }

    /**
     * Rechaza la petición con el mismo problem+json que responde el resto de la
     * plataforma.
     *
     * Antes era un setComplete(): estado 401 y cero bytes de cuerpo. El contrato de
     * errores dice que un cliente solo decide por la extensión {@code code}, así que
     * la respuesta más frecuente que ve —la sesión caducada— era justo la única que
     * no sabía leer, y la web la mostraba como un fallo de red.
     *
     * Los tres caminos que llegan aquí escriben un cuerpo idéntico byte a byte. Que
     * falte la cabecera, que el token no verifique o que sus claims no se puedan leer
     * son distinciones que sirven para el log; contarlas en la respuesta convertiría
     * al gateway en un oráculo.
     */
    private Mono<Void> unauthenticated(ServerWebExchange exchange) {
        return problemDetailWriter.write(exchange, GatewayProblem.UNAUTHENTICATED);
    }
}