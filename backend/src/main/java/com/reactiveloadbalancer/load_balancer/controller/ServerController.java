package com.reactiveloadbalancer.load_balancer.controller;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.service.LoadBalancerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ServerController {

    private final LoadBalancerService loadBalancerService;

    @GetMapping("/servers")
    public Mono<List<BackendServer>> listServers() {
        return Mono.just(loadBalancerService.getAllServers());
    }

    @PostMapping("/servers")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BackendServer> registerServer(@RequestBody BackendServer server) {
        BackendServer registered = loadBalancerService.registerServer(server);
        log.info("Registered server via API: id={} url={}", registered.getId(), registered.getUrl());
        return Mono.just(registered);
    }

    @DeleteMapping("/servers/{id}")
    public Mono<ResponseEntity<Void>> deregisterServer(@PathVariable String id) {
        boolean removed = loadBalancerService.deregisterServer(id);
        return Mono.just(removed
                ? ResponseEntity.noContent().<Void>build()
                : ResponseEntity.notFound().<Void>build());
    }

    @GetMapping("/servers/{id}")
    public Mono<ResponseEntity<BackendServer>> getServer(@PathVariable String id) {
        return loadBalancerService.getAllServers().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .map(s -> Mono.just(ResponseEntity.ok(s)))
                .orElseGet(() -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/config/strategy")
    public Mono<StrategyResponse> getStrategy() {
        return Mono.just(new StrategyResponse(
                loadBalancerService.getActiveStrategyName(),
                loadBalancerService.getAvailableStrategies()
        ));
    }

    @PatchMapping("/config/strategy")
    public Mono<ResponseEntity<SetStrategyResponse>> setStrategy(@Valid @RequestBody SetStrategyRequest request)
    {
        try {
            loadBalancerService.setStrategyByName(request.strategy());
            log.info("Strategy switched via API to: {}", request.strategy());
            return Mono.just(ResponseEntity.ok(new SetStrategyResponse(request.strategy())));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to switch strategy to: {}", request.strategy(), e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
}