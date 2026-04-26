package com.reactiveloadbalancer.load_balancer.strategy;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RoundRobinStrategy implements LoadBalancerStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Optional<BackendServer> selectServer(List<BackendServer> healthyServers) {
        if (healthyServers == null || healthyServers.isEmpty()) {
            log.warn("RoundRobin: no healthy servers available");
            return Optional.empty();
        }

        int size = healthyServers.size();
        int index = Math.abs(counter.getAndIncrement() % size);
        BackendServer selected = healthyServers.get(index);
        log.debug("RoundRobin: selected server index={} id={} url={}", index, selected.getId(), selected.getUrl());
        return Optional.of(selected);
    }

    @Override
    public String getName() {
        return "ROUND_ROBIN";
    }

    public void reset() {
        counter.set(0);
    }
}
