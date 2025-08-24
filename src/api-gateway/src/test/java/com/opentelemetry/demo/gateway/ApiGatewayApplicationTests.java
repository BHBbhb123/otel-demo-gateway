// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}
