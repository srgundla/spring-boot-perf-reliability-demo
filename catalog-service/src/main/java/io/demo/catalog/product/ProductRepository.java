package io.demo.catalog.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Classic OFFSET pagination: also runs a COUNT(*) query. Gets slower the deeper the page. */
    Page<Product> findAllByOrderByIdAsc(Pageable pageable);

    /** Keyset ("seek") pagination: uses the primary-key index, cost is flat regardless of depth. */
    @Query("""
            select p.id as id, p.sku as sku, p.name as name, p.priceCents as priceCents
            from Product p
            where p.id > :afterId
            order by p.id
            """)
    List<ProductSummary> findNextPage(@Param("afterId") long afterId, Pageable pageable);

    /**
     * Stand-in for an expensive query (big join / aggregation). pg_sleep adds a fixed, honest delay so the
     * benefit of caching is visible on a laptop where a primary-key lookup is otherwise ~1 ms.
     */
    @Query(value = "select p.* from products p cross join pg_sleep(:delaySeconds) where p.id = :id",
            nativeQuery = true)
    Optional<Product> findByIdAfterSimulatedWork(@Param("id") long id, @Param("delaySeconds") double delaySeconds);
}
