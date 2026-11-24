package com.mybuilding.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre global de Rate Limiting (Token Bucket en mémoire par IP client et route).
 * Jalon V6 : Protection de l'API Gateway contre les rafales de requêtes (HTTP 429).
 */
@Component
public class InMemoryRateLimiterFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryRateLimiterFilter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InMemoryRateLimiterFilter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!properties.isEnabled() || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientIp(request) + ":" + resolveRouteKey(exchange, path);
        TokenBucket bucket = buckets.computeIfAbsent(
                clientKey,
                key -> new TokenBucket(properties.getCapacity(), properties.getRefillTokens(), properties.getRefillSeconds(), clock)
        );

        TokenBucket.ConsumptionResult result = bucket.tryConsume();
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(properties.getCapacity()));
        response.getHeaders().set("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (result.allowed()) {
            return chain.filter(exchange);
        }

        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Limite de requêtes atteinte sur l'API Gateway. Réessayez dans %d seconde(s).","path":"%s"}
                """.formatted(LocalDateTime.now(clock), result.retryAfterSeconds(), escapeJson(path)).trim();

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -10;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String resolveRouteKey(ServerWebExchange exchange, String path) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && route.getId() != null) {
            return route.getId();
        }
        String[] segments = path.split("/");
        if (segments.length > 2 && "api".equals(segments[1])) {
            return "api-" + segments[2];
        }
        return segments.length > 1 && !segments[1].isEmpty() ? segments[1] : "root";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static final class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final long refillIntervalMillis;
        private final Clock clock;
        private int availableTokens;
        private long lastRefillMillis;

        TokenBucket(int capacity, int refillTokens, int refillSeconds, Clock clock) {
            this.capacity = Math.max(1, capacity);
            this.refillTokens = Math.max(1, refillTokens);
            this.refillIntervalMillis = Math.max(1, refillSeconds) * 1000L;
            this.clock = clock;
            this.availableTokens = this.capacity;
            this.lastRefillMillis = clock.millis();
        }

        synchronized ConsumptionResult tryConsume() {
            long now = clock.millis();
            long elapsed = now - lastRefillMillis;
            if (elapsed >= refillIntervalMillis) {
                long windows = elapsed / refillIntervalMillis;
                availableTokens = (int) Math.min(capacity, availableTokens + windows * refillTokens);
                lastRefillMillis += windows * refillIntervalMillis;
            }

            if (availableTokens > 0) {
                availableTokens--;
                return new ConsumptionResult(true, availableTokens, 0);
            }

            long remainingMillis = Math.max(1000L, refillIntervalMillis - (now - lastRefillMillis));
            long retryAfter = (remainingMillis + 999L) / 1000L;
            return new ConsumptionResult(false, 0, retryAfter);
        }

        record ConsumptionResult(boolean allowed, int remaining, long retryAfterSeconds) {}
    }
}
