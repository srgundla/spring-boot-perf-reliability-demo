package io.demo.orders.catalog;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import org.springframework.web.client.HttpClientErrorException;

/** Circuit breaker settings in one place so the unit test exercises exactly what production uses. */
public final class CatalogBreaker {

    private CatalogBreaker() {
    }

    public static CircuitBreaker create() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // A 404 / 400 is the caller's problem, not evidence that the catalog is unhealthy.
                .ignoreExceptions(HttpClientErrorException.class)
                .build();
        return CircuitBreaker.of("catalog", config);
    }
}
