package io.demo.orders.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tiny admin view so the live demo can show breaker state with a single curl. */
@RestController
@RequestMapping("/admin/breaker")
public class BreakerController {

    private final CircuitBreaker breaker;

    public BreakerController(CircuitBreaker catalogBreaker) {
        this.breaker = catalogBreaker;
    }

    @GetMapping
    public View view() {
        var m = breaker.getMetrics();
        return new View(breaker.getName(), breaker.getState().name(), m.getFailureRate(),
                m.getNumberOfBufferedCalls(), m.getNumberOfNotPermittedCalls());
    }

    public record View(String name, String state, float failureRatePercent, int bufferedCalls, long notPermittedCalls) {
    }
}
