package com.reactiveloadbalancer.load_balancer.controller;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import com.reactiveloadbalancer.load_balancer.service.LoadBalancerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
    public Mono<Map<String, Object>> getStrategy() {
        return Mono.just(Map.of(
                "active", loadBalancerService.getActiveStrategyName(),
                "available", loadBalancerService.getAvailableStrategies()
        ));
    }

    @PatchMapping("/config/strategy")
    public Mono<ResponseEntity<Map<String, String>>> setStrategy(@RequestBody Map<String, String> body) {
        String name = body.get("strategy");
        if (name == null || name.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'strategy' field")));
        }
        try {
            loadBalancerService.setStrategyByName(name);
            log.info("Strategy switched via API to: {}", name);
            return Mono.just(ResponseEntity.ok(Map.of("active", name)));
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage())));
        }
    }
}