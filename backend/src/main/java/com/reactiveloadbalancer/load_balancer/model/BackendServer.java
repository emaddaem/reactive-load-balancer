package com.reactiveloadbalancer.load_balancer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BackendServer {

    private String id;
    
    private String url;
    
    @Builder.Default
    private int weight = 1;
    
    @Builder.Default
    private volatile boolean healthy = true;
    
    @JsonIgnore
    @Builder.Default
    private AtomicInteger activeConnections = new AtomicInteger(0);

    private volatile LocalDateTime lastHealthCheck;
    
    @JsonIgnore
    @Builder.Default
    private AtomicInteger consecutiveFailures = new AtomicInteger(0);
    
    @JsonIgnore
    @Builder.Default
    private AtomicLong totalRequests = new AtomicLong(0);
    
    @JsonIgnore
    @Builder.Default
    private AtomicLong failedRequests = new AtomicLong(0);
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private volatile String lastError;
    
    private String description;

    // Connection Management
        
    public int incrementConnections() {
        int newCount = activeConnections.incrementAndGet();
        log.debug("Server {} connections: {} -> {}", id, newCount - 1, newCount);
        return newCount;
    }
    
    public int decrementConnections() {
        int newCount = activeConnections.decrementAndGet();
        if (newCount < 0) {
            log.warn("Server {} has negative connection count: {}", id, newCount);
            activeConnections.set(0);
            return 0;
        }
        log.debug("Server {} connections: {} -> {}", id, newCount + 1, newCount);
        return newCount;
    }
    
    public int getConnectionCount() {
        return activeConnections.get();
    }

    // Health Management
    
    public void markHealthy() {
        if (!this.healthy) {
            log.info("Server {} ({}) is now HEALTHY", id, url);
        }
        this.healthy = true;
        this.consecutiveFailures.set(0);
        this.lastHealthCheck = LocalDateTime.now();
        this.lastError = null;
    }
    
    public void recordHealthCheckFailure(String errorMessage, int failureThreshold) {
        int failures = this.consecutiveFailures.incrementAndGet();
        this.lastHealthCheck = LocalDateTime.now();
        this.lastError = errorMessage;
        
        if (failures >= failureThreshold && this.healthy) {
            log.warn("Server {} ({}) is now UNHEALTHY after {} consecutive failures. Last error: {}", 
                     id, url, failures, errorMessage);
            this.healthy = false;
        }
    }
    
    public void markUnhealthy(String reason) {
        log.warn("Server {} ({}) manually marked UNHEALTHY. Reason: {}", id, url, reason);
        this.healthy = false;
        this.lastError = reason;
    }
    
    // Request Metrics
    
    public void recordSuccessfulRequest() {
        totalRequests.incrementAndGet();
    }
    
    public void recordFailedRequest() {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }
    
    public double getSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 100.0;
        }
        long failed = failedRequests.get();
        return ((total - failed) * 100.0) / total;
    }
    
    public long getTotalRequestCount() {
        return totalRequests.get();
    }
    
    public long getFailedRequestCount() {
        return failedRequests.get();
    }
    
    // Availability Checks
    
    public boolean isAvailable() {
        return healthy;
    }
    
    public boolean isOverloaded(int maxConnections) {
        return activeConnections.get() >= maxConnections;
    }
    
    // Utility Methods
    
    public void resetMetrics() {
        totalRequests.set(0);
        failedRequests.set(0);
        activeConnections.set(0);
        consecutiveFailures.set(0);
        log.info("Metrics reset for server {} ({})", id, url);
    }
    
    public String getStatusSummary() {
        return String.format("Server[id=%s, url=%s, healthy=%s, connections=%d, requests=%d, successRate=%.2f%%]", id, url, healthy, activeConnections.get(), 
                             totalRequests.get(), getSuccessRate());
    }
    
    public void validate() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Server URL must start with http:// or https://");
        }
        if (weight < 1 || weight > 100) {
            throw new IllegalArgumentException("Server weight must be between 1 and 100");
        }
    }
}