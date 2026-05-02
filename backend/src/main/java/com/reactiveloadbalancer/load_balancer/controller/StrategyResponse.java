package com.reactiveloadbalancer.load_balancer.controller;

import java.util.Set;

public record StrategyResponse(String active, Set<String> available) {}