package com.reactiveloadbalancer.load_balancer.strategy;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class LeastConnectionsStrategy implements LoadBalancerStrategy {

    @Override
    public Optional<BackendServer> selectServer(List<BackendServer> healthyServers) {
        if (healthyServers == null || healthyServers.isEmpty()) {
            log.warn("LeastConnections: no healthy servers available");
            return Optional.empty();
        }

        BackendServer selected = healthyServers.stream()
                .min(Comparator.comparingInt(s -> s.getActiveConnections().get()))
                .orElseThrow();

        log.debug("LeastConnections: selected id={} url={} activeConnections={}",
                selected.getId(), selected.getUrl(), selected.getActiveConnections().get());

        return Optional.of(selected);
    }

    @Override
    public String getName() {
        return "LEAST_CONNECTIONS";
    }
}