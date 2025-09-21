create table if not exists users (
  id             uuid primary key,
  first_name     varchar(100) not null,
  last_name      varchar(100) not null,
  email          varchar(320) not null,
  password_hash  varchar(100) not null,
  is_active      boolean not null default true,
  created_at     timestamp not null,
  updated_at     timestamp not null,
  version        bigint not null
);

create unique index if not exists uq_users_email on users(email);

create index if not exists idx_users_created_at on users(created_at);--I doing for popular search
create index if not exists idx_users_is_active  on users(is_active);
--in real system I will add the index:
--create index if not exists idx_users_created_at_id on users (created_at desc, id desc);
