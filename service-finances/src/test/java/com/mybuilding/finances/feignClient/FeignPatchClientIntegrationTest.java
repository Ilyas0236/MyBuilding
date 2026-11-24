package com.mybuilding.finances.feignClient;

import com.sun.net.httpserver.HttpServer;
import feign.Client;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.hc5.ApacheHttp5Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FeignPatchClientIntegrationTest {

    @Autowired
    private Client feignClient;

    @Test
    void shouldUseApacheHttp5ClientAndExecuteHttpPatchWithoutProtocolException() throws Exception {
        Client delegate = feignClient instanceof FeignBlockingLoadBalancerClient lb
                ? lb.getDelegate()
                : feignClient;

        assertInstanceOf(ApacheHttp5Client.class, delegate,
                "OpenFeign doit déléguer à ApacheHttp5Client (feign-hc5) pour supporter HTTP PATCH");

        AtomicReference<String> receivedMethod = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/residents/1/solde", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            PatchTestClient client = Feign.builder()
                    .client(delegate)
                    .target(PatchTestClient.class, "http://localhost:" + port);

            client.updateSolde(1L, "{\"delta\":-150.00}");
            assertEquals("PATCH", receivedMethod.get());
            assertEquals("{\"delta\":-150.00}", receivedBody.get());
        } finally {
            server.stop(0);
        }
    }

    interface PatchTestClient {
        @RequestLine("PATCH /api/residents/{id}/solde")
        @Headers("Content-Type: application/json")
        void updateSolde(@Param("id") Long id, String payload);
    }
}
