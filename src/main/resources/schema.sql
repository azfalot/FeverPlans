create table if not exists plans (
    id uuid primary key,
    base_plan_id varchar(255) not null,
    provider_plan_id varchar(255) not null,
    title varchar(255) not null,
    starts_at timestamp not null,
    ends_at timestamp not null,
    min_price numeric(12, 2),
    max_price numeric(12, 2),
    constraint uq_plan_provider_identity unique (base_plan_id, provider_plan_id)
);

create index if not exists idx_plans_starts_at on plans (starts_at);
create index if not exists idx_plans_ends_at on plans (ends_at);
