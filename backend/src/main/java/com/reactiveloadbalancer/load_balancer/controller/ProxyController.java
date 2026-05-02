package com.reactiveloadbalancer.load_balancer.controller;

import com.reactiveloadbalancer.load_balancer.config.WebClientConfig;
import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.service.LoadBalancerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    private final LoadBalancerService loadBalancerService;
    private final WebClient webClient;
    private final WebClientConfig webClientConfig;

    @RequestMapping("/**")
    public Mono<Void> proxy(ServerWebExchange exchange) {
        ServerHttpRequest request  = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        return loadBalancerService.getNextServer()
                .map(server -> forwardTo(server, request, response))
                .orElseGet(() -> {
                    log.warn("No healthy backends — returning 503 for {}", request.getPath());
                    response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return response.setComplete();
                });
    }

    private Mono<Void> forwardTo(BackendServer server, ServerHttpRequest request, ServerHttpResponse response) {
        String targetUrl = server.getUrl() + request.getPath().value();
        String query = request.getURI().getRawQuery();
        if (query != null && !query.isBlank()) {
            targetUrl += "?" + query;
        }

        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        log.info("[{}] Proxying {} {} -> {}", traceId, request.getMethod(), request.getPath(), targetUrl);

        server.incrementConnections();

        return webClient.method(request.getMethod())
                .uri(URI.create(targetUrl))
                .headers(headers -> {
                    headers.addAll(request.getHeaders());
                    headers.remove("Host"); // let WebClient set the correct Host header
                    if (traceId != null) {
                        headers.set("X-Trace-Id", traceId);
                    }
                    // Standard forwarding headers
                    headers.set("X-Forwarded-For", request.getRemoteAddress() != null
                            ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown");
                    headers.set("X-Forwarded-Host", request.getURI().getHost());
                })
                .body(request.getBody(), org.springframework.core.io.buffer.DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    response.setStatusCode(clientResponse.statusCode());
                    response.getHeaders().addAll(clientResponse.headers().asHttpHeaders());
                    server.recordSuccessfulRequest();
                    log.info("[{}] Response {} from {}", traceId, clientResponse.statusCode(), server.getUrl());
                    return response.writeWith(clientResponse.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class));
                })
                .timeout(webClientConfig.proxyTimeout())
                .doOnError(ex -> {
                    log.error("[{}] Proxy error to {}: {}", traceId, server.getUrl(), ex.getMessage());
                    server.recordFailedRequest();
                    response.setStatusCode(HttpStatus.BAD_GATEWAY);
                })
                .doFinally(signal -> server.decrementConnections())
                .onErrorResume(ex -> response.setComplete());
    }
}