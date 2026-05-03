package com.reactiveloadbalancer.load_balancer.service;

import com.reactiveloadbalancer.load_balancer.config.LoadBalancerProperties;
import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.strategy.LoadBalancerStrategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class LoadBalancerService {

    private final ServerRegistry serverRegistry;
    private final CircuitBreakerService circuitBreakerService;
    private volatile LoadBalancerStrategy activeStrategy;
    private final Map<String, LoadBalancerStrategy> strategiesByName;

    public LoadBalancerService(ServerRegistry serverRegistry, CircuitBreakerService circuitBreakerService, Map<String, LoadBalancerStrategy> strategies, LoadBalancerProperties properties) {
        this.serverRegistry = serverRegistry;
        this.circuitBreakerService = circuitBreakerService;
        this.strategiesByName = new HashMap<>();
        strategies.values().forEach(s -> strategiesByName.put(s.getName(), s));
        String defaultName = properties.getAlgorithm().getDefaultAlgorithm();
        this.activeStrategy = strategiesByName.values().stream()
                .filter(s -> s.getName().equals(defaultName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown default strategy: " + defaultName));

        log.info("LoadBalancerService initialized with strategy: {}", activeStrategy.getName());
    }

    public Optional<BackendServer> getNextServer() {
        List<BackendServer> healthyServers = serverRegistry.getHealthy();
        Optional<BackendServer> selected = activeStrategy.selectServer(healthyServers);
        if (selected.isEmpty()) {
            log.warn("No healthy servers available to handle request");
        }
        return selected;
    }

    public void setStrategy(LoadBalancerStrategy strategy) {
        log.info("Switching load balancing strategy: {} -> {}", activeStrategy.getName(), strategy.getName());
        this.activeStrategy = strategy;
    }

    public void setStrategyByName(String name) {
        LoadBalancerStrategy next = strategiesByName.get(name);
        if (next == null) {
            throw new IllegalArgumentException("Unknown strategy: " + name + ". Available: " + strategiesByName.keySet());
        }
        log.info("Switching load balancing strategy: {} -> {}", activeStrategy.getName(), name);
        this.activeStrategy = next;
    }

    public Set<String> getAvailableStrategies() {
        return Collections.unmodifiableSet(strategiesByName.keySet());
    }

    public String getActiveStrategyName() {
        return activeStrategy.getName();
    }

    public BackendServer registerServer(BackendServer server) {
        return serverRegistry.register(server);
    }

    public boolean deregisterServer(String id) {
        boolean removed = serverRegistry.deregister(id);
        if (removed) {
            circuitBreakerService.remove(id);
        }
        return removed;
    }

    public List<BackendServer> getAllServers() {
        return serverRegistry.getAll();
    }

    public List<BackendServer> getHealthyServers() {
        return serverRegistry.getHealthy();
    }
}
