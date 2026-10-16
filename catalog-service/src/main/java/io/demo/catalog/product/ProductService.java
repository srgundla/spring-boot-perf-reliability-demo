package io.demo.catalog.product;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    public static final String CACHE = "product";

    private final ProductRepository repository;
    private final double expensiveQuerySeconds;

    public ProductService(ProductRepository repository,
                          @Value("${demo.expensive-query-ms:25}") long expensiveQueryMs) {
        this.repository = repository;
        this.expensiveQuerySeconds = expensiveQueryMs / 1000.0;
    }

    /** Always goes to PostgreSQL. */
    @Transactional(readOnly = true)
    public ProductDto loadDirect(long id) {
        return fetch(id);
    }

    /**
     * Cache-aside via Spring Cache + Redis. sync=true means that when many requests miss on the same key
     * at once, only ONE of them hits the database (cache-stampede protection).
     */
    @Cacheable(cacheNames = CACHE, key = "#id", sync = true)
    @Transactional(readOnly = true)
    public ProductDto loadCached(long id) {
        return fetch(id);
    }

    /** Write path: update the row, then evict the cached copy so readers never see a stale price for long. */
    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public ProductDto updatePrice(long id, long priceCents) {
        Product product = repository.findById(id).orElseThrow(() -> notFound(id));
        product.setPriceCents(priceCents);
        return ProductDto.from(product);
    }

    @Transactional(readOnly = true)
    public PagedProducts offsetPage(int page, int size) {
        var result = repository.findAllByOrderByIdAsc(PageRequest.of(page, size));
        List<ProductSummaryDto> items = result.getContent().stream()
                .map(p -> new ProductSummaryDto(p.getId(), p.getSku(), p.getName(), p.getPriceCents()))
                .toList();
        return new PagedProducts(items, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PagedProducts keysetPage(long afterId, int size) {
        List<ProductSummaryDto> items = repository.findNextPage(afterId, PageRequest.of(0, size)).stream()
                .map(p -> new ProductSummaryDto(p.getId(), p.getSku(), p.getName(), p.getPriceCents()))
                .toList();
        return new PagedProducts(items, -1);
    }

    private ProductDto fetch(long id) {
        return repository.findByIdAfterSimulatedWork(id, expensiveQuerySeconds)
                .map(ProductDto::from)
                .orElseThrow(() -> notFound(id));
    }

    private static ResponseStatusException notFound(long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "product " + id + " not found");
    }

    public record ProductSummaryDto(long id, String sku, String name, long priceCents) {
    }

    /** total = -1 means "not computed" (keyset pagination deliberately skips COUNT(*)). */
    public record PagedProducts(List<ProductSummaryDto> items, long total) {
    }
}
