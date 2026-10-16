-- 500,000 synthetic products. Big enough that deep OFFSET pagination is measurably slower than keyset.
insert into products (sku, name, category, price_cents, stock)
select 'SKU-' || lpad(g::text, 7, '0'),
       'Product ' || g,
       (array['books','electronics','home','toys','garden','sports','grocery','fashion'])[1 + (g % 8)],
       500 + ((g * 37) % 99500),
       (g * 13) % 500
from generate_series(1, 500000) as g;

create unique index idx_products_sku on products (sku);
create index idx_products_category_id on products (category, id);

analyze products;
