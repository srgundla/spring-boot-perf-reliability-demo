package io.demo.catalog.product;

import java.util.function.Supplier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Served through the Redis cache. */
    @GetMapping("/{id}")
    public Timed<ProductDto> cached(@PathVariable long id) {
        return timed("redis-cache-or-db", () -> service.loadCached(id));
    }

    /** Same data, always straight from PostgreSQL - use it as the "before" picture. */
    @GetMapping("/{id}/direct")
    public Timed<ProductDto> direct(@PathVariable long id) {
        return timed("postgres", () -> service.loadDirect(id));
    }

    @PutMapping("/{id}/price")
    public ProductDto updatePrice(@PathVariable long id, @RequestParam long cents) {
        return service.updatePrice(id, cents);
    }

    @GetMapping("/offset")
    public Timed<ProductService.PagedProducts> offset(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "5") int size) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return timed("OFFSET " + ((long) page * safeSize) + " + COUNT(*)",
                () -> service.offsetPage(Math.max(page, 0), safeSize));
    }

    @GetMapping("/keyset")
    public Timed<ProductService.PagedProducts> keyset(@RequestParam(defaultValue = "0") long afterId,
                                                      @RequestParam(defaultValue = "5") int size) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return timed("WHERE id > " + afterId, () -> service.keysetPage(afterId, safeSize));
    }

    private static <T> Timed<T> timed(String source, Supplier<T> work) {
        long start = System.nanoTime();
        T data = work.get();
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        return new Timed<>(source, tookMs, data);
    }

    /** Wraps every response with where the data came from and how long the server-side work took (numbers first, so they are easy to spot). */
    public record Timed<T>(String source, long tookMs, T data) {
    }
}
