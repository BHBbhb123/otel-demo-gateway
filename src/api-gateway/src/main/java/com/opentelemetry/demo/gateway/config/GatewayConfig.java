// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

package com.opentelemetry.demo.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Gateway configuration for routing and filtering
 */
@Configuration
public class GatewayConfig {

    @Value("${services.ad.url:http://frontend:8080}")
    private String adServiceUrl;

    @Value("${services.cart.url:http://frontend:8080}")
    private String cartServiceUrl;

    @Value("${services.checkout.url:http://frontend:8080}")
    private String checkoutServiceUrl;

    @Value("${services.currency.url:http://frontend:8080}")
    private String currencyServiceUrl;

    @Value("${services.email.url:http://email:6060}")
    private String emailServiceUrl;

    @Value("${services.payment.url:http://frontend:8080}")
    private String paymentServiceUrl;

    @Value("${services.product-catalog.url:http://frontend:8080}")
    private String productCatalogServiceUrl;

    @Value("${services.quote.url:http://quote:8090}")
    private String quoteServiceUrl;

    @Value("${services.recommendation.url:http://frontend:8080}")
    private String recommendationServiceUrl;

    @Value("${services.shipping.url:http://shipping:50050}")
    private String shippingServiceUrl;

    @Value("${services.frontend.url:http://frontend:8080}")
    private String frontendServiceUrl;

    @Value("${services.frontend-proxy.url:http://frontend-proxy:8080}")
    private String frontendProxyServiceUrl;

    @Value("${services.jaeger.url:http://jaeger:16686}")
    private String jaegerServiceUrl;

    @Value("${services.grafana.url:http://grafana:3000}")
    private String grafanaServiceUrl;

    @Value("${services.prometheus.url:http://prometheus:9090}")
    private String prometheusServiceUrl;

    @Value("${services.load-generator.url:http://load-generator:8089}")
    private String loadGeneratorServiceUrl;

    @Value("${services.otel-collector.url:http://otel-collector:4318}")
    private String otelCollectorServiceUrl;

    @Value("${services.image-provider.url:http://image-provider:8080}")
    private String imageProviderServiceUrl;

    @Value("${services.flagd.url:http://flagd:8013}")
    private String flagdServiceUrl;

    @Value("${services.flagd-ui.url:http://flagd-ui:3000}")
    private String flagdUiServiceUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Load Generator UI
                .route("loadgen-ui", r -> r.path("/loadgen/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(loadGeneratorServiceUrl))

                // OpenTelemetry Collector HTTP
                .route("otlp-http", r -> r.path("/otlp-http/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(otelCollectorServiceUrl))

                // Monitoring and Admin Routes
                .route("jaeger-ui", r -> r.path("/jaeger/**")
                        .uri(jaegerServiceUrl))
                
                .route("grafana-ui", r -> r.path("/grafana/**")
                        .uri(grafanaServiceUrl))

                // Frontend Static Assets (higher priority - more specific paths first)
                .route("frontend-static", r -> r.path("/_next/**", "/favicon.ico", "/icons/**", "/static/**", "/assets/**", "*.css", "*.js", "*.png", "*.jpg", "*.svg", "*.ico")
                        .uri(frontendServiceUrl))
                
                // Handle double slash URLs for frontend assets
                .route("frontend-static-double-slash", r -> r.path("//icons/**", "//_next/**", "//static/**", "//assets/**")
                        .filters(f -> f.rewritePath("//(?<segment>.*)", "/${segment}"))
                        .uri(frontendServiceUrl))

                // Image Provider Service
                .route("images", r -> r.path("/images/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(imageProviderServiceUrl))

                // Feature Flag Services
                .route("flagservice", r -> r.path("/flagservice/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(flagdServiceUrl))

                .route("flagd-ui", r -> r.path("/feature/**")
                        .uri(flagdUiServiceUrl))
                
                .route("frontend-pages", r -> r.path("/", "/cart", "/product/**", "/checkout", "/about", "/contact", "/account/**")
                        .uri(frontendServiceUrl))
                
                // Catch-all route for SPA routing (lowest priority)
                .route("frontend-spa-fallback", r -> r.path("/**")
                        .and().not(p -> p.path("/api/**"))
                        .and().not(p -> p.path("/grafana/**"))
                        .and().not(p -> p.path("/jaeger/**"))
                        .and().not(p -> p.path("/loadgen/**"))
                        .and().not(p -> p.path("/images/**"))
                        .and().not(p -> p.path("/health"))
                        .uri(frontendServiceUrl))

                // Health check route
                .route("health-route", r -> r.path("/health")
                        .uri("forward:/actuator/health"))
                
                // Backend API Services
                // gRPC服务 - 通过前端代理
                .route("ad-service", r -> r.path("/api/ads/**")
                        .uri(adServiceUrl))

                // Cart Service Routes (gRPC - via frontend)
                .route("cart-service", r -> r.path("/api/cart/**")
                        .uri(cartServiceUrl))

                // Checkout Service Routes (gRPC - via frontend)
                .route("checkout-service", r -> r.path("/api/checkout/**")
                        .uri(checkoutServiceUrl))

                // Currency Service Routes (gRPC - via frontend)
                .route("currency-service", r -> r.path("/api/currency/**")
                        .uri(currencyServiceUrl))

                // Email Service Routes (HTTP)
                .route("email-service", r -> r.path("/api/email/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(emailServiceUrl))

                // Payment Service Routes (gRPC - via frontend)
                .route("payment-service", r -> r.path("/api/payment/**")
                        .uri(paymentServiceUrl))

                // Product Catalog Service Routes (gRPC - via frontend)
                .route("product-catalog-service", r -> r.path("/api/products/**")
                        .uri(productCatalogServiceUrl))

                // Quote Service Routes
                .route("quote-service", r -> r.path("/api/quote/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(quoteServiceUrl))

                // Recommendation Service Routes (gRPC - via frontend)
                .route("recommendation-service", r -> r.path("/api/recommendations/**")
                        .uri(recommendationServiceUrl))

                // Shipping Service Routes – GET /api/shipping 转交前端 API 路由
                .route("frontend-shipping-api", r -> r.path("/api/shipping")
                        .and().method("GET")
                        .uri(frontendServiceUrl))

                // Shipping Service Routes (HTTP)
                .route("shipping-service", r -> r.path("/api/shipping/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(shippingServiceUrl))

                // Data (Ads) Service Routes – Next.js API route
                .route("data-service", r -> r.path("/api/data", "/api/data/**")
                        .uri(frontendServiceUrl))

                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}