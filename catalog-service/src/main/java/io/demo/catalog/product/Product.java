package io.demo.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String name;
    private String category;

    @Column(name = "price_cents")
    private long priceCents;

    private int stock;

    protected Product() {
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getPriceCents() { return priceCents; }
    public int getStock() { return stock; }

    public void setPriceCents(long priceCents) { this.priceCents = priceCents; }
}
