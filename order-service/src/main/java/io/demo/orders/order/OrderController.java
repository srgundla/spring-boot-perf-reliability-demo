package io.demo.orders.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;
    private final OrderQueryService queries;

    public OrderController(OrderService orders, OrderQueryService queries) {
        this.orders = orders;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderService.OrderResponse place(@RequestBody OrderService.PlaceOrderRequest request) {
        return orders.place(request);
    }

    /** N+1 version: look at "sqlStatements" in the response. */
    @GetMapping("/slow")
    public OrderQueryService.Measured<List<OrderQueryService.OrderSummary>> slow(
            @RequestParam(defaultValue = "50") int size) {
        return queries.latestSlow(Math.clamp(size, 1, 500));
    }

    /** Fixed version: same data, constant number of statements. */
    @GetMapping("/fast")
    public OrderQueryService.Measured<List<OrderQueryService.OrderSummary>> fast(
            @RequestParam(defaultValue = "50") int size) {
        return queries.latestFast(Math.clamp(size, 1, 500));
    }
}
