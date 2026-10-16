create table products (
    id           bigserial primary key,
    sku          varchar(32)  not null,
    name         varchar(200) not null,
    category     varchar(40)  not null,
    price_cents  bigint       not null,
    stock        integer      not null
);
