// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Logging filter for API Gateway
 * Logs incoming requests and outgoing responses
 */
@Component
public class LoggingFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        // Log incoming request
        logRequest(exchange, requestId);
        
        // Add request ID to headers
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(r -> r.header("X-Request-ID", requestId))
                .build();

        return chain.filter(modifiedExchange)
                .doOnSuccess(aVoid -> logResponse(modifiedExchange, requestId, startTime))
                .doOnError(throwable -> logError(modifiedExchange, requestId, startTime, throwable));
    }

    private void logRequest(ServerWebExchange exchange, String requestId) {
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getQueryParams().toString();
        String remoteAddress = getClientIpAddress(exchange);
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");

        logger.info("Request ID: {} | Method: {} | Path: {} | Query: {} | Client IP: {} | User-Agent: {}",
                requestId, method, path, query, remoteAddress, userAgent);
    }

    private void logResponse(ServerWebExchange exchange, String requestId, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        int statusCode = exchange.getResponse().getStatusCode() != null ? 
                exchange.getResponse().getStatusCode().value() : 0;

        logger.info("Response ID: {} | Status: {} | Duration: {}ms",
                requestId, statusCode, duration);
    }

    private void logError(ServerWebExchange exchange, String requestId, long startTime, Throwable throwable) {
        long duration = System.currentTimeMillis() - startTime;
        
        logger.error("Error ID: {} | Duration: {}ms | Error: {}",
                requestId, duration, throwable.getMessage(), throwable);
    }

    private String getClientIpAddress(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
