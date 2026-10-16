package io.demo.orders.catalog;

import java.util.Optional;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resilience pipeline for one logical catalog lookup:
 * circuit breaker -> (retry + bulkhead inside CatalogHttp) -> timeouts -> HTTP call.
 * Anything that goes wrong becomes a graceful fallback (empty result), never an exception for the caller.
 */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final CatalogHttp http;
    private final CircuitBreaker breaker;
    private final MeterRegistry registry;

    public CatalogClient(CatalogHttp http, CircuitBreaker catalogBreaker, MeterRegistry registry) {
        this.http = http;
        this.breaker = catalogBreaker;
        this.registry = registry;
    }

    public Optional<CatalogProduct> findProduct(long productId) {
        try {
            return Optional.of(breaker.executeSupplier(() -> http.fetch(productId)));
        } catch (CallNotPermittedException e) {
            // Circuit is OPEN: fail fast, do not even try the network.
            return fallback("breaker_open", productId, e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown product " + productId);
        } catch (RuntimeException e) {
            return fallback("error", productId, e);
        }
    }

    private Optional<CatalogProduct> fallback(String reason, long productId, Exception cause) {
        registry.counter("demo.orders.fallback", "reason", reason).increment();
        log.warn("catalog lookup for product {} degraded ({}): {}", productId, reason, cause.toString());
        return Optional.empty();
    }
}
