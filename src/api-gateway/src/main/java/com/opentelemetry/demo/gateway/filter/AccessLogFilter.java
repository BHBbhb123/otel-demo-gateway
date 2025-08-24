package com.opentelemetry.demo.gateway.filter;

import com.opentelemetry.demo.gateway.model.GatewayLog;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ReactiveHttpOutputMessage;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import org.springframework.util.StreamUtils;

/**
 * 访问日志过滤器，记录网关请求与响应参数。
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    // 该顺序必须小于 -1，确保在响应写出之前处理
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getPath().pathWithinApplication().value();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        GatewayLog gatewayLog = new GatewayLog();
        gatewayLog.setSchema(request.getURI().getScheme());
        gatewayLog.setRequestMethod(request.getMethod() != null ? request.getMethod().name() : "UNKNOWN");
        gatewayLog.setRequestPath(requestPath);
        if (route != null) {
            gatewayLog.setTargetServer(route.getId());
        }
        gatewayLog.setRequestTime(new Date());
        gatewayLog.setIp(getClientIpAddress(request));

        MediaType mediaType = request.getHeaders().getContentType();

        boolean isJsonRequest = mediaType != null && MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
        boolean bodyMethod = request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT || request.getMethod() == HttpMethod.PATCH;

        if (bodyMethod) {
            // 仅当 JSON 请求体才记录
            if (isJsonRequest) {
                return writeBodyLog(exchange, chain, gatewayLog);
            } else {
                return chain.filter(exchange);
            }
        } else {
            // GET / DELETE 等无 body 请求，直接记录（主要关心 JSON 响应）
            return writeBasicLog(exchange, chain, gatewayLog);
        }
    }

    /**
     * 处理有 body 的请求，解决流只能读取一次问题
     */
    private Mono<Void> writeBodyLog(ServerWebExchange exchange, GatewayFilterChain chain, GatewayLog gatewayLog) {
        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    gatewayLog.setRequestBody(body);
                    return Mono.just(body);
                });

        BodyInserter<?, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext())
                .then(Mono.defer(() -> {
                    ServerHttpRequest decoratedRequest = requestDecorate(exchange, headers, outputMessage);
                    ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, gatewayLog);
                    return chain.filter(exchange.mutate().request(decoratedRequest).response(decoratedResponse).build())
                            .then(Mono.fromRunnable(() -> writeAccessLog(gatewayLog)));
                }));
    }

    /**
     * 处理无 body 的请求，记录查询参数并包装响应
     */
    private Mono<Void> writeBasicLog(ServerWebExchange exchange, GatewayFilterChain chain, GatewayLog gatewayLog) {
        StringBuilder builder = new StringBuilder();
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        queryParams.forEach((k, v) -> builder.append(k).append("=").append(StringUtils.collectionToDelimitedString(v, ",")));
        gatewayLog.setRequestBody(builder.toString());

        ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, gatewayLog);
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .then(Mono.fromRunnable(() -> writeAccessLog(gatewayLog)));
    }

    private void writeAccessLog(GatewayLog gatewayLog) {
        log.info(gatewayLog.toString());
    }

    private ServerHttpRequestDecorator requestDecorate(ServerWebExchange exchange, HttpHeaders headers,
                                                       CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(headers);
                long contentLength = headers.getContentLength();
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }

    private ServerHttpResponseDecorator recordResponseLog(ServerWebExchange exchange, GatewayLog gatewayLog) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Date responseTime = new Date();
                    gatewayLog.setResponseTime(responseTime);
                    gatewayLog.setExecuteTime(responseTime.getTime() - gatewayLog.getRequestTime().getTime());

                    String originalResponseContentType = exchange.getAttribute(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                    if (HttpStatus.OK.equals(getStatusCode()) && originalResponseContentType != null && !originalResponseContentType.startsWith("image") && originalResponseContentType.contains("application/json")) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                        return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                            DataBuffer join = new DefaultDataBufferFactory().join(dataBuffers);
                            byte[] rawContent = new byte[join.readableByteCount()];
                            join.read(rawContent);
                            DataBufferUtils.release(join);

                            // 用于记录的内容（默认使用原始字节）
                            byte[] logBytes = rawContent;

                            String contentEncoding = originalResponse.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
                            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(rawContent));
                                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                    StreamUtils.copy(gis, bos);
                                    logBytes = bos.toByteArray();
                                } catch (Exception e) {
                                    log.warn("Failed to decompress gzip response for logging", e);
                                }
                            }

                            gatewayLog.setResponseData(new String(logBytes, StandardCharsets.UTF_8));

                            // 始终把原始（可能仍为 gzip 压缩）字节返回给客户端
                            return bufferFactory.wrap(rawContent);
                        }));
                    }
                }
                return super.writeWith(body);
            }
        };
    }

    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
