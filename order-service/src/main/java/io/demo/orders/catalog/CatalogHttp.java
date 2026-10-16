package io.demo.orders.catalog;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * The raw HTTP call to catalog-service, protected by two Spring Framework 7 built-in resilience features:
 * <ul>
 *   <li>{@code @ConcurrencyLimit} - bulkhead: at most 20 calls in flight, the rest wait (never floods catalog)</li>
 *   <li>{@code @Retryable} - 2 retries with exponential backoff + jitter, only for timeouts / connection errors / 5xx</li>
 * </ul>
 * Built from the auto-configured RestClient.Builder so trace context (traceparent header) is propagated.
 */
@Component
public class CatalogHttp {

    private final RestClient rest;

    public CatalogHttp(RestClient.Builder builder,
                       @Value("${demo.catalog.base-url}") String baseUrl,
                       @Value("${demo.catalog.connect-timeout-ms:300}") long connectTimeoutMs,
                       @Value("${demo.catalog.read-timeout-ms:800}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.rest = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @ConcurrencyLimit(20)
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 100, multiplier = 2, jitter = 50)
    public CatalogProduct fetch(long productId) {
        CatalogProduct.Envelope envelope = rest.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(CatalogProduct.Envelope.class);
        if (envelope == null || envelope.data() == null) {
            throw new IllegalStateException("empty response from catalog");
        }
        return envelope.data();
    }
}
