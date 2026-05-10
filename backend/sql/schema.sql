create schema if not exists core;
create schema if not exists shard_0;
create schema if not exists shard_1;

create table if not exists shard_0.users (
  id text primary key,
  full_name text not null,
  email text not null unique,
  password_hash text not null,
  balance numeric(12, 2) not null default 0 check (balance >= 0),
  two_factor_enabled boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists shard_1.users (like shard_0.users including all);

create table if not exists shard_0.products (
  id text primary key,
  owner_id text not null,
  name text not null,
  brand text not null,
  category text not null,
  description text not null,
  price numeric(12, 2) not null check (price > 0),
  quantity integer not null check (quantity >= 0),
  condition text not null check (condition in ('New', 'Like New', 'Used')),
  status text not null check (status in ('listed', 'sold', 'draft')),
  listed_at timestamptz not null default now(),
  sold_count integer not null default 0 check (sold_count >= 0),
  color text not null default '#0f766e'
);

create table if not exists shard_1.products (like shard_0.products including all);

create index if not exists shard_0_products_owner_idx on shard_0.products(owner_id);
create index if not exists shard_1_products_owner_idx on shard_1.products(owner_id);
create index if not exists shard_0_products_search_idx on shard_0.products(lower(name), lower(brand), category);
create index if not exists shard_1_products_search_idx on shard_1.products(lower(name), lower(brand), category);

create table if not exists core.auth_challenges (
  id text primary key,
  user_id text not null,
  code text not null,
  purpose text not null check (purpose in ('register')),
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists core.purchase_challenges (
  id text primary key,
  buyer_id text not null,
  seller_id text not null,
  product_id text not null,
  amount numeric(12, 2) not null check (amount > 0),
  code text not null,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists core.purchases (
  id text primary key,
  product_id text not null,
  product_name text not null,
  buyer_id text not null,
  seller_id text not null,
  amount numeric(12, 2) not null check (amount > 0),
  purchased_at timestamptz not null default now(),
  status text not null check (status in ('completed', 'pending', 'failed'))
);

create table if not exists core.transactions (
  id text primary key,
  type text not null check (type in ('deposit', 'purchase', 'sale', 'refund')),
  amount numeric(12, 2) not null check (amount >= 0),
  from_user_id text,
  to_user_id text,
  product_id text,
  description text not null,
  created_at timestamptz not null default now(),
  status text not null check (status in ('completed', 'pending', 'failed'))
);

create index if not exists core_transactions_user_idx on core.transactions(from_user_id, to_user_id);
create index if not exists core_transactions_product_idx on core.transactions(product_id);
create index if not exists core_purchases_user_idx on core.purchases(buyer_id, seller_id);

comment on schema core is 'Shared coordination schema for transactions, purchases, 2FA challenges, and reports.';
comment on schema shard_0 is 'Logical data shard 0 for users and products.';
comment on schema shard_1 is 'Logical data shard 1 for users and products.';
