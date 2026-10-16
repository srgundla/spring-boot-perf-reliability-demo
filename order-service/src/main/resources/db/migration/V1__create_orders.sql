create table customer_orders (
    id           bigserial primary key,
    customer_id  varchar(64) not null,
    status       varchar(32) not null,
    total_cents  bigint      not null default 0,
    created_at   timestamptz not null default now()
);

create table order_items (
    id                bigserial primary key,
    order_id          bigint  not null references customer_orders (id),
    product_id        bigint  not null,
    quantity          integer not null,
    unit_price_cents  bigint  not null
);

create index idx_order_items_order_id on order_items (order_id);
