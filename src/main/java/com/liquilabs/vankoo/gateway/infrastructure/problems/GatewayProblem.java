package com.liquilabs.vankoo.gateway.infrastructure.problems;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * The failures the gateway itself can report, in the shape RFC 9457 asks for.
 *
 * It is the smaller twin of {@code ApiProblem} in vankoo-iam-service, and it is
 * deliberately small: the gateway only reports what it decides on its own. Anything a
 * service answered travels back untouched, because by the time a response reaches
 * here the reason behind it is already gone — turning a 500 into a 409 is a decision
 * only whoever knows the business rule can make.
 *
 * That leaves exactly one entry. A request that never got past the bearer filter was
 * never seen by any service, so this is the one error the gateway must author itself.
 */
public enum GatewayProblem {

    // One entry for all three ways the filter can reject a request — no header, a
    // token that does not verify, a token whose claims cannot be read. The contract
    // requires authentication failures to be indistinguishable, and no client would
    // do anything different: all three mean "sign in again".
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated", "Unauthenticated",
            "This endpoint requires a valid bearer token.");

    /** Prefix of the {@code type} URI, the same one every service uses. */
    public static final String TYPE_BASE = "https://docs.vankoo.dev/errors/";

    /** Name of the extension member clients switch on. */
    public static final String CODE_PROPERTY = "code";

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String detail;

    GatewayProblem(HttpStatus status, String code, String title, String detail) {
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public ProblemDetail toProblemDetail(URI instance) {
        var problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setType(URI.create(TYPE_BASE + code));
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(instance);
        problemDetail.setProperty(CODE_PROPERTY, code);
        return problemDetail;
    }
}
