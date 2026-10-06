package com.liquilabs.vankoo.gateway.infrastructure.problems;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes a problem+json body straight onto the reactive response.
 *
 * A gateway filter has no controller advice behind it and no message converter to
 * render a return value — and this gateway has no Spring Security either, so there is
 * not even an authentication entry point to hook. The body has to be written by hand,
 * which is the same conclusion the servlet side reached for its security handlers.
 *
 * The body is assembled as an explicit map rather than handed to the mapper as a
 * {@link ProblemDetail}, whose serialisation leans on a Jackson mixin registered only
 * on the MVC path. Spelling it out keeps the gateway's 401 byte-compatible with the
 * one IAM writes for the same case.
 */
@Component
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, GatewayProblem problem) {
        var response = exchange.getResponse();
        response.setStatusCode(problem.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        // RFC 9110 requires it on a 401, and it is what tells a client which scheme to
        // retry with rather than leaving it to guess.
        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);

        var detail = problem.toProblemDetail(URI.create(exchange.getRequest().getPath().value()));
        // Deferred so the buffer is allocated when the response is actually written,
        // and released by the framework if the subscription never happens.
        return response.writeWith(Mono.fromSupplier(
                () -> response.bufferFactory().wrap(objectMapper.writeValueAsBytes(asBody(detail)))));
    }

    private Map<String, Object> asBody(ProblemDetail detail) {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", detail.getType().toString());
        body.put("title", detail.getTitle());
        body.put("status", detail.getStatus());
        body.put("detail", detail.getDetail());
        body.put("instance", detail.getInstance() == null ? null : detail.getInstance().toString());
        var properties = detail.getProperties();
        if (properties != null) body.putAll(properties);
        return body;
    }

    private static final String BEARER_CHALLENGE = "Bearer";
}
