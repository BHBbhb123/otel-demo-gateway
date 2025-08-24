// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller for API Gateway
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> response = Map.of(
                "status", "UP",
                "service", "api-gateway",
                "timestamp", Instant.now().toString(),
                "version", "1.0.0"
        );
        
        return Mono.just(ResponseEntity.ok(response));
    }

    @GetMapping("/ready")
    public Mono<ResponseEntity<Map<String, Object>>> ready() {
        Map<String, Object> response = Map.of(
                "status", "READY",
                "service", "api-gateway",
                "timestamp", Instant.now().toString()
        );
        
        return Mono.just(ResponseEntity.ok(response));
    }
}
