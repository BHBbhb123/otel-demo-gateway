# Copyright The OpenTelemetry Authors
# SPDX-License-Identifier: Apache-2.0

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user (Alpine Linux syntax)
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Install necessary packages for health check
RUN apk add --no-cache curl

# Copy the pre-built JAR file directly
COPY src/api-gateway/target/api-gateway-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
