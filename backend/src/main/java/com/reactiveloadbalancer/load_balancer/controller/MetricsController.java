package com.reactiveloadbalancer.load_balancer.controller;

import com.reactiveloadbalancer.load_balancer.model.MetricsSnapshot;
import com.reactiveloadbalancer.load_balancer.service.LoadBalancerService;
import com.reactiveloadbalancer.load_balancer.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;
    private final LoadBalancerService loadBalancerService;

    @GetMapping
    public Mono<MetricsSnapshot> getMetrics() {
        return Mono.just(metricsService.snapshot());
    }

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getSummary() {
        MetricsSnapshot snap = metricsService.snapshot();
        return Mono.just(Map.of(
                "totalServers",       snap.getTotalServers(),
                "healthyServers",     snap.getHealthyServers(),
                "unhealthyServers",   snap.getUnhealthyServers(),
                "totalRequests",      snap.getTotalRequests(),
                "activeConnections",  snap.getTotalActiveConnections(),
                "activeStrategy",     loadBalancerService.getActiveStrategyName(),
                "timestamp",          snap.getTimestamp()
        ));
    }
}