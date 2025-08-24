// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Error handling and debugging endpoints
 */
@RestController
public class ErrorController {

    @GetMapping("/gateway-info")
    public Mono<ResponseEntity<Map<String, Object>>> root() {
        Map<String, Object> response = Map.of(
                "service", "API Gateway",
                "status", "running",
                "timestamp", Instant.now().toString(),
                "version", "1.0.0",
                "message", "OpenTelemetry Demo API Gateway is running",
                "endpoints", Map.of(
                        "health", "/health",
                        "actuator", "/actuator/health", 
                        "products", "/api/products",
                        "test", "/test",
                        "gateway-info", "/gateway-info"
                )
        );
        
        return Mono.just(ResponseEntity.ok(response));
    }

    @GetMapping("/debug")
    public Mono<ResponseEntity<Map<String, Object>>> debug() {
        Map<String, Object> response = Map.of(
                "gateway_info", "OpenTelemetry Demo API Gateway",
                "available_routes", Map.of(
                        "GET /gateway-info", "Gateway info",
                        "GET /health", "Health check",
                        "GET /test", "Test route",
                        "GET /api/products", "Product catalog",
                        "GET /api/ads", "Advertisement service",
                        "GET /api/cart", "Shopping cart",
                        "POST /api/checkout", "Checkout process"
                ),
                "timestamp", Instant.now().toString()
        );
        
        return Mono.just(ResponseEntity.ok(response));
    }
}
