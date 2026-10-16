package io.demo.catalog.product;

import java.io.Serializable;

/**
 * Cached value. Implements Serializable so the default Redis cache serializer can store it
 * (keeps the demo free of custom serializer configuration).
 */
public record ProductDto(long id, String sku, String name, String category, long priceCents, int stock)
        implements Serializable {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getPriceCents(), p.getStock());
    }
}
