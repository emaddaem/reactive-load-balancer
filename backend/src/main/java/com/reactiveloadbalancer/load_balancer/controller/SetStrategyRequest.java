package com.reactiveloadbalancer.load_balancer.controller;

import jakarta.validation.constraints.NotBlank;

public record SetStrategyRequest(@NotBlank(message = "Strategy name cannot be blank") String strategy) {}
