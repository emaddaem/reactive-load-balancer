package com.reactiveloadbalancer.load_balancer.service;

import com.reactiveloadbalancer.load_balancer.config.LoadBalancerProperties;
import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final ServerRegistry serverRegistry;
    private final WebClient webClient;
    private final LoadBalancerProperties properties;

    @Scheduled(fixedDelayString = "${loadbalancer.health.check.interval:10000}")
    public void checkAll() {
        serverRegistry.getAll().forEach(this::check);
    }

    private void check(BackendServer server) {
        String healthUrl = server.getUrl() + "/health";
        long timeoutMs = properties.getHealth().getCheck().getTimeout();
        int failureThreshold = properties.getFailureThreshold();

        webClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(timeoutMs))
                .subscribe(
                        response -> {
                            if (response.getStatusCode().is2xxSuccessful()) {
                                if (!server.isHealthy()) {
                                    log.info("Server {} ({}) recovered — marking HEALTHY", server.getId(), server.getUrl());
                                }
                                server.markHealthy();
                            } else {
                                String msg = "Non-2xx health check response: " + response.getStatusCode();
                                log.warn("Server {} ({}) health check failed: {}", server.getId(), server.getUrl(), msg);
                                server.recordHealthCheckFailure(msg, failureThreshold);
                            }
                        },
                        error -> {
                            String msg = error.getClass().getSimpleName() + ": " + error.getMessage();
                            log.warn("Server {} ({}) health check error: {}", server.getId(), server.getUrl(), msg);
                            server.recordHealthCheckFailure(msg, failureThreshold);
                        }
                );
    }
}