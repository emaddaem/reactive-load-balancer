package com.reactiveloadbalancer.load_balancer.service;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.model.MetricsSnapshot;
import com.reactiveloadbalancer.load_balancer.model.MetricsSnapshot.ServerMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ServerRegistry serverRegistry;

    public MetricsSnapshot snapshot() {
        List<BackendServer> all = serverRegistry.getAll();

        long totalRequests = all.stream().mapToLong(s -> s.getTotalRequests().get()).sum();
        long totalFailed   = all.stream().mapToLong(s -> s.getFailedRequests().get()).sum();
        int  activeConns   = all.stream().mapToInt(s -> s.getActiveConnections().get()).sum();
        int  healthyCount  = (int) all.stream().filter(BackendServer::isHealthy).count();

        List<ServerMetrics> serverMetrics = all.stream()
                .map(s -> ServerMetrics.builder()
                        .id(s.getId())
                        .url(s.getUrl())
                        .healthy(s.isHealthy())
                        .activeConnections(s.getActiveConnections().get())
                        .totalRequests(s.getTotalRequests().get())
                        .failedRequests(s.getFailedRequests().get())
                        .successRate(s.getSuccessRate())
                        .lastError(s.getLastError())
                        .build())
                .toList();

        return MetricsSnapshot.builder()
                .timestamp(Instant.now().toEpochMilli())
                .totalServers(all.size())
                .healthyServers(healthyCount)
                .unhealthyServers(all.size() - healthyCount)
                .totalRequests(totalRequests)
                .totalFailedRequests(totalFailed)
                .totalActiveConnections(activeConns)
                .servers(serverMetrics)
                .build();
    }
}