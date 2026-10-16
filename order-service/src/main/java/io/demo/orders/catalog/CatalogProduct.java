package io.demo.orders.catalog;

/** Only the fields order-service needs from the catalog (consumer-driven, tolerant reader). */
public record CatalogProduct(long id, String sku, String name, long priceCents) {

    /** Matches the catalog's {"source": "...", "tookMs": n, "data": {...}} response wrapper. */
    public record Envelope(CatalogProduct data) {
    }
}
