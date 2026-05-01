package com.reactiveloadbalancer.load_balancer.strategy;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class WeightedRoundRobinStrategy implements LoadBalancerStrategy {

    private final Map<String, Integer> currentWeights = new HashMap<>();

    @Override
    public synchronized Optional<BackendServer> selectServer(List<BackendServer> healthyServers) {
        if (healthyServers == null || healthyServers.isEmpty()) {
            log.warn("WeightedRoundRobin: no healthy servers available");
            return Optional.empty();
        }

        int totalWeight = healthyServers.stream().mapToInt(BackendServer::getWeight).sum();

        for (BackendServer server : healthyServers) {
            currentWeights.merge(server.getId(), server.getWeight(), (a, b) -> a + b);
        }

        BackendServer selected = healthyServers.stream()
                .max(Comparator.comparingInt(s -> currentWeights.getOrDefault(s.getId(), 0)))
                .orElseThrow();

        currentWeights.put(selected.getId(), currentWeights.get(selected.getId()) - totalWeight);

        log.debug("WeightedRoundRobin: selected id={} url={} weight={} currentWeight={}",
                selected.getId(), selected.getUrl(), selected.getWeight(),
                currentWeights.get(selected.getId()));

        return Optional.of(selected);
    }

    @Override
    public String getName() {
        return "WEIGHTED_ROUND_ROBIN";
    }

    public synchronized void reset() {
        currentWeights.clear();
    }
}
