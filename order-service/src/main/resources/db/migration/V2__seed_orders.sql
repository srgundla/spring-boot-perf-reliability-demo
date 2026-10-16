-- 5,000 historical orders x 3 items each, so the N+1 demo has real data to chew on.
insert into customer_orders (customer_id, status, total_cents)
select 'cust-' || (g % 500), 'CONFIRMED', 0
from generate_series(1, 5000) as g;

insert into order_items (order_id, product_id, quantity, unit_price_cents)
select o.id, 1 + ((o.id * 7 + i) % 499999), 1 + (i % 3), 1000 + (i * 100)
from customer_orders o
cross join generate_series(1, 3) as i;

update customer_orders o
set total_cents = (select coalesce(sum(quantity * unit_price_cents), 0) from order_items i where i.order_id = o.id);

analyze customer_orders;
analyze order_items;
