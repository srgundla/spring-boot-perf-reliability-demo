package io.demo.orders.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.demo.orders.catalog.CatalogBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    CircuitBreaker catalogBreaker(MeterRegistry registry) {
        CircuitBreaker breaker = CatalogBreaker.create();

        // 0 = CLOSED (healthy), 1 = OPEN (failing fast), 2 = HALF_OPEN (probing)
        Gauge.builder("demo.circuitbreaker.state", breaker, b -> stateValue(b.getState()))
                .tag("name", breaker.getName())
                .register(registry);

        breaker.getEventPublisher().onStateTransition(event ->
                log.warn("circuit breaker '{}' state change: {}", breaker.getName(), event.getStateTransition()));
        return breaker;
    }

    private static double stateValue(CircuitBreaker.State state) {
        return switch (state) {
            case CLOSED -> 0;
            case OPEN -> 1;
            case HALF_OPEN -> 2;
            default -> 3;
        };
    }
}
