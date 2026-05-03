package com.reactiveloadbalancer.load_balancer.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CircuitBreakerService {

    private final CircuitBreakerRegistry registry;
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreakerService() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(60f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();

        this.registry = CircuitBreakerRegistry.of(config);
    }

    public CircuitBreaker forServer(String serverId) {
        return breakers.computeIfAbsent(serverId, id -> {
            CircuitBreaker cb = registry.circuitBreaker(id);
            cb.getEventPublisher().onStateTransition(event ->
                    log.warn("Circuit breaker [{}] state: {} -> {}",
                            id,
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
            return cb;
        });
    }

    public void remove(String serverId) {
        breakers.remove(serverId);
        registry.remove(serverId);
    }
}