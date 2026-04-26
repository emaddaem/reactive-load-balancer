package com.reactiveloadbalancer.load_balancer.service;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ServerRegistry {

    private final ConcurrentHashMap<String, BackendServer> servers = new ConcurrentHashMap<>();

    public BackendServer register(BackendServer server) {
        if (server.getId() == null || server.getId().isBlank()) {
            server.setId(UUID.randomUUID().toString());
        }
        servers.put(server.getId(), server);
        log.info("Registered server: id={} url={}", server.getId(), server.getUrl());
        return server;
    }

    public boolean deregister(String id) {
        BackendServer removed = servers.remove(id);
        if (removed != null) {
            log.info("Deregistered server: id={} url={}", id, removed.getUrl());
            return true;
        }
        log.warn("Attempted to deregister unknown server id={}", id);
        return false;
    }

    public Optional<BackendServer> findById(String id) {
        return Optional.ofNullable(servers.get(id));
    }

    public List<BackendServer> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(servers.values()));
    }

    public List<BackendServer> getHealthy() {
        List<BackendServer> healthy = servers.values().stream()
                .filter(BackendServer::isHealthy)
                .toList();
        log.debug("Healthy servers: {}/{}", healthy.size(), servers.size());
        return healthy;
    }

    public int count() {
        return servers.size();
    }

    public int healthyCount() {
        return (int) servers.values().stream().filter(BackendServer::isHealthy).count();
    }
}
