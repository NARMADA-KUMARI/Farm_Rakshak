package com.farmrakshak.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        String finalTraceId = traceId;
        exchange.getRequest().mutate().header(TRACE_ID_HEADER, traceId);
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        log.info("Request: {} {} | TraceId: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                finalTraceId
        );

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
