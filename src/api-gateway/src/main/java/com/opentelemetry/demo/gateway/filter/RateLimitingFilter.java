// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting filter for API Gateway
 * Implements token bucket algorithm using Redis with in-memory fallback
 */
@Component
public class RateLimitingFilter implements GatewayFilter {

    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);

    @Autowired(required = false)
    private ReactiveStringRedisTemplate redisTemplate;
    
    // In-memory fallback for rate limiting when Redis is not available
    private final ConcurrentHashMap<String, AtomicLong> inMemoryCounters = new ConcurrentHashMap<>();
    private volatile long lastResetTime = System.currentTimeMillis();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = getClientIdentifier(exchange);
        
        if (redisTemplate != null) {
            return applyRedisRateLimit(exchange, chain, clientId);
        } else {
            return applyInMemoryRateLimit(exchange, chain, clientId);
        }
    }
    
    private Mono<Void> applyRedisRateLimit(ServerWebExchange exchange, GatewayFilterChain chain, String clientId) {
        String key = "rate_limit:" + clientId;

        return redisTemplate.opsForValue()
                .increment(key)
                .cast(Long.class)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, WINDOW_SIZE)
                                .then(Mono.just(count));
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> processRateLimit(exchange, chain, count))
                .onErrorResume(throwable -> applyInMemoryRateLimit(exchange, chain, clientId));
    }
    
    private Mono<Void> applyInMemoryRateLimit(ServerWebExchange exchange, GatewayFilterChain chain, String clientId) {
        // Reset counters every minute
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > WINDOW_SIZE.toMillis()) {
            inMemoryCounters.clear();
            lastResetTime = currentTime;
        }
        
        AtomicLong counter = inMemoryCounters.computeIfAbsent(clientId, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();
        
        return processRateLimit(exchange, chain, count);
    }
    
    private Mono<Void> processRateLimit(ServerWebExchange exchange, GatewayFilterChain chain, long count) {
        if (count > DEFAULT_REQUESTS_PER_MINUTE) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                    String.valueOf(DEFAULT_REQUESTS_PER_MINUTE));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            return exchange.getResponse().setComplete();
        } else {
            long remaining = DEFAULT_REQUESTS_PER_MINUTE - count;
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                    String.valueOf(DEFAULT_REQUESTS_PER_MINUTE));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                    String.valueOf(remaining));
            return chain.filter(exchange);
        }
    }

    private String getClientIdentifier(ServerWebExchange exchange) {
        // Try to get session ID first
        String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-ID");
        if (sessionId != null && !sessionId.isEmpty()) {
            return "session:" + sessionId;
        }

        // Fall back to IP address
        String clientIp = getClientIpAddress(exchange);
        return "ip:" + clientIp;
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
