package com.mybuilding.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRateLimiterFilterTest {

    @Test
    void shouldAllowWithinCapacityThenReturn429AndRefillAfterWindow() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setCapacity(3);
        props.setRefillTokens(3);
        props.setRefillSeconds(10);

        MutableClock clock = new MutableClock(Instant.parse("2026-11-24T20:00:00Z"));
        InMemoryRateLimiterFilter filter = new InMemoryRateLimiterFilter(props, clock);
        GatewayFilterChain chain = exchange -> Mono.empty();

        for (int i = 0; i < 3; i++) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/residents").header("X-Forwarded-For", "127.0.0.1").build()
            );
            filter.filter(exchange, chain).block();
            assertNull(exchange.getResponse().getStatusCode());
            assertEquals("3", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
            assertEquals(String.valueOf(2 - i), exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        }

        // 4ème requête dans la même fenêtre -> 429 Too Many Requests
        MockServerWebExchange blocked = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/residents").header("X-Forwarded-For", "127.0.0.1").build()
        );
        filter.filter(blocked, chain).block();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getResponse().getStatusCode());
        assertEquals("0", blocked.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertNotNull(blocked.getResponse().getHeaders().getFirst("Retry-After"));

        // Avance de 10 secondes -> tokens rechargés
        clock.advanceSeconds(10);
        MockServerWebExchange refilled = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/residents").header("X-Forwarded-For", "127.0.0.1").build()
        );
        filter.filter(refilled, chain).block();
        assertNull(refilled.getResponse().getStatusCode());
        assertEquals("2", refilled.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
