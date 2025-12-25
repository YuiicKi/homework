package com.ruangong.config;

import com.ruangong.model.JwtPayload;
import com.ruangong.service.JwtService;
import graphql.GraphqlErrorException;
import java.net.InetSocketAddress;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtGraphQlInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtGraphQlInterceptor.class);

    private final JwtService jwtService;

    public JwtGraphQlInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String token = authHeader.substring(7);
            try {
                JwtPayload payload = jwtService.parseToken(token);
                request.configureExecutionInput((executionInput, builder) ->
                    builder.graphQLContext(ctxBuilder -> ctxBuilder.put("currentUser", payload)).build());
            } catch (Exception ex) {
                String requestId = request.getId();
                String clientIp = resolveClientIp(request);
                log.warn("JWT validation failed for request {} from {}: {}", requestId, clientIp, ex.getMessage());
                return Mono.error(GraphqlErrorException.newErrorException()
                    .errorClassification(ErrorType.UNAUTHORIZED)
                    .message("Token 无效或已过期")
                    .build());
            }
        }
        return chain.next(request);
    }

    private String resolveClientIp(WebGraphQlRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getHostString();
        }
        return request.getHeaders().getFirst("X-Forwarded-For");
    }
}
