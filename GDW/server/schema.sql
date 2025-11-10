create extension if not exists pgcrypto;
create table if not exists letters(
  id uuid primary key default gen_random_uuid(),
  org_id text not null,
  body text not null,
  category text,
  created_at timestamptz default now(),
  pow_ok boolean default false,
  device_group text,
  hash_onchain text,
  tx_hash text,
  status text default 'new'
);
create index if not exists idx_letters_org_created on letters(org_id, created_at);
