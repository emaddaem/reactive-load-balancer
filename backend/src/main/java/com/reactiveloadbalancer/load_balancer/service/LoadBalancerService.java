package com.reactiveloadbalancer.load_balancer.service;

import com.reactiveloadbalancer.load_balancer.config.LoadBalancerProperties;
import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.strategy.LoadBalancerStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class LoadBalancerService {

    private final ServerRegistry serverRegistry;
    private volatile LoadBalancerStrategy activeStrategy;

    public LoadBalancerService( ServerRegistry serverRegistry, Map<String, LoadBalancerStrategy> strategies, LoadBalancerProperties properties) {
        this.serverRegistry = serverRegistry;
        this.activeStrategy = strategies.values().stream()
            .filter(s -> s.getName().equals(properties.getAlgorithm().getDefaultAlgorithm()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown default strategy: " + properties.getAlgorithm().getDefaultAlgorithm()));
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

    public String getActiveStrategyName() {
        return activeStrategy.getName();
    }

    public BackendServer registerServer(BackendServer server) {
        return serverRegistry.register(server);
    }

    public boolean deregisterServer(String id) {
        return serverRegistry.deregister(id);
    }

    public List<BackendServer> getAllServers() {
        return serverRegistry.getAll();
    }

    public List<BackendServer> getHealthyServers() {
        return serverRegistry.getHealthy();
    }
}
