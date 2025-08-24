// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Authentication filter for API Gateway
 * Validates authentication tokens and user sessions
 */
@Component
public class AuthenticationFilter implements GatewayFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/health",
            "/actuator",
            "/api/products",
            "/api/currency",
            "/api/ads"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip authentication for public endpoints
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // Check for session ID or authorization header
        String sessionId = extractSessionId(exchange);
        if (sessionId == null || sessionId.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Add session information to request headers for downstream services
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(r -> r.header("X-Session-ID", sessionId))
                .build();

        return chain.filter(modifiedExchange);
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractSessionId(ServerWebExchange exchange) {
        // Try to get session ID from header
        String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-ID");
        
        // Try to get from query parameter
        if (sessionId == null) {
            sessionId = exchange.getRequest().getQueryParams().getFirst("sessionId");
        }
        
        // Try to get from Authorization header (Bearer token)
        if (sessionId == null) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                sessionId = authHeader.substring(7);
            }
        }

        return sessionId;
    }
}
