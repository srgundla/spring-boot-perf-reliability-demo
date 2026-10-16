package io.demo.orders.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class CatalogBreakerTest {

    @Test
    void opensAfterRepeatedFailuresAndThenFailsFast() {
        CircuitBreaker breaker = CatalogBreaker.create();

        for (int i = 0; i < 5; i++) {
            try {
                breaker.executeSupplier(() -> {
                    throw new IllegalStateException("catalog down");
                });
            } catch (IllegalStateException expected) {
                // counted as a failure by the breaker
            }
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> breaker.executeSupplier(() -> "never reached"))
                .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    void clientErrorsDoNotTripTheBreaker() {
        CircuitBreaker breaker = CatalogBreaker.create();

        for (int i = 0; i < 20; i++) {
            try {
                breaker.executeSupplier(() -> {
                    throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nope", null, null, null);
                });
            } catch (HttpClientErrorException expected) {
                // ignored by the breaker configuration
            }
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
