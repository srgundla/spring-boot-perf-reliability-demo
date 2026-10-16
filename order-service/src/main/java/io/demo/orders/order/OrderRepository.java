package io.demo.orders.order;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /** Used by the SLOW path: items are lazy, so touching them later fires one extra query per order (N+1). */
    Slice<CustomerOrder> findAllBy(Pageable pageable);

    /** FAST path, step 1: page over ids only (cheap, no collection fetch, so pagination stays in SQL). */
    @Query("select o.id from CustomerOrder o order by o.id desc")
    List<Long> findLatestIds(Pageable pageable);

    /** FAST path, step 2: one JOIN FETCH query loads those orders together with all their items. */
    @Query("""
            select o from CustomerOrder o
            left join fetch o.items
            where o.id in :ids
            order by o.id desc
            """)
    List<CustomerOrder> findWithItems(@Param("ids") List<Long> ids);
}
