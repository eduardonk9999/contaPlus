create table stores (
    id         uuid        primary key,
    name       text        not null,
    currency   char(3)     not null default 'BRL',
    timezone   text        not null default 'America/Sao_Paulo',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
)