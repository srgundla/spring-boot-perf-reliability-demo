package io.demo.orders.order;

import java.util.List;
import java.util.function.Supplier;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Same API result, two implementations: N+1 queries vs. 2 queries.
 * SQL statements are counted with Hibernate statistics (global counter - accurate when you run one request
 * at a time, which is how the demo uses it).
 */
@Service
public class OrderQueryService {

    private final OrderRepository repository;
    private final Statistics statistics;

    public OrderQueryService(OrderRepository repository, EntityManagerFactory entityManagerFactory) {
        this.repository = repository;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Transactional(readOnly = true)
    public Measured<List<OrderSummary>> latestSlow(int size) {
        return measure(() -> repository.findAllBy(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().stream()
                // getItems().size() initialises the lazy collection -> one extra SELECT per order
                .map(o -> new OrderSummary(o.getId(), o.getCustomerId(), o.getItems().size(), o.getTotalCents()))
                .toList());
    }

    @Transactional(readOnly = true)
    public Measured<List<OrderSummary>> latestFast(int size) {
        return measure(() -> {
            List<Long> ids = repository.findLatestIds(PageRequest.of(0, size));
            if (ids.isEmpty()) {
                return List.<OrderSummary>of();
            }
            return repository.findWithItems(ids).stream()
                    .map(o -> new OrderSummary(o.getId(), o.getCustomerId(), o.getItems().size(), o.getTotalCents()))
                    .toList();
        });
    }

    private <T> Measured<T> measure(Supplier<T> work) {
        long statementsBefore = statistics.getPrepareStatementCount();
        long start = System.nanoTime();
        T data = work.get();
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        return new Measured<>(statistics.getPrepareStatementCount() - statementsBefore, tookMs, data);
    }

    public record OrderSummary(long id, String customerId, int itemCount, long totalCents) {
    }

    public record Measured<T>(long sqlStatements, long tookMs, T data) {
    }
}
