package com.reactiveloadbalancer.load_balancer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MetricsSnapshot {

    long timestamp;

    int totalServers;

    int healthyServers;

    int unhealthyServers;

    long totalRequests;

    long totalFailedRequests;

    int totalActiveConnections;

    List<ServerMetrics> servers;

    @Value
    @Builder
    public static class ServerMetrics {
        String id;
        String url;
        boolean healthy;
        int activeConnections;
        long totalRequests;
        long failedRequests;
        double successRate;
        String lastError;
    }
}