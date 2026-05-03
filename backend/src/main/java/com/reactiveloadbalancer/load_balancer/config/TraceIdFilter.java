package com.reactiveloadbalancer.load_balancer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class TraceIdFilter implements WebFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        final String finalTraceId = traceId;
        log.debug("[{}] {} {}", finalTraceId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TRACE_ID_KEY, finalTraceId));
    }
}