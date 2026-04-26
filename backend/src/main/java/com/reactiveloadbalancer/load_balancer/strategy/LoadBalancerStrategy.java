package com.reactiveloadbalancer.load_balancer.strategy;

import com.reactiveloadbalancer.load_balancer.model.BackendServer;

import java.util.List;
import java.util.Optional;


public interface LoadBalancerStrategy {

    Optional<BackendServer> selectServer(List<BackendServer> healthyServers);

    String getName();
}
