package io.demo.orders.order;

import java.util.Optional;

import io.demo.orders.catalog.CatalogClient;
import io.demo.orders.catalog.CatalogProduct;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final CatalogClient catalog;

    public OrderService(OrderRepository repository, CatalogClient catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    /**
     * Deliberately NOT @Transactional: the remote call to the catalog must not hold a database connection
     * while it waits. Only repository.save() opens a (short) transaction.
     */
    public OrderResponse place(PlaceOrderRequest request) {
        if (request.quantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
        }

        Optional<CatalogProduct> product = catalog.findProduct(request.productId());
        boolean degraded = product.isEmpty();
        long unitPrice = product.map(CatalogProduct::priceCents).orElse(0L);

        var order = new CustomerOrder(request.customerId(), degraded ? "PENDING_PRICE_CHECK" : "CONFIRMED");
        order.addItem(new OrderItem(request.productId(), request.quantity(), unitPrice));
        order.setTotalCents(unitPrice * request.quantity());
        CustomerOrder saved = repository.save(order);

        return new OrderResponse(saved.getId(), saved.getStatus(), saved.getTotalCents(), degraded);
    }

    public record PlaceOrderRequest(String customerId, long productId, int quantity) {
    }

    /** degraded = true means the catalog was unavailable and we accepted the order with a deferred price. */
    public record OrderResponse(long id, String status, long totalCents, boolean degraded) {
    }
}
