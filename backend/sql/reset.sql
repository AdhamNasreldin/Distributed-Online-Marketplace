drop schema if exists core cascade;
drop schema if exists shard_0 cascade;
drop schema if exists shard_1 cascade;

\i sql/schema.sql
\i sql/seed.sql
