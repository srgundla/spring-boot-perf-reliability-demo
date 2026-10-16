package io.demo.catalog.product;

/** Interface projection: Hibernate selects only these four columns instead of the whole entity. */
public interface ProductSummary {
    Long getId();

    String getSku();

    String getName();

    Long getPriceCents();
}
