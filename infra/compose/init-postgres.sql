-- Runs on first Postgres initialization only (empty data directory).
-- The default 'delivery_eta' DB is created via POSTGRES_DB env var.
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE delivery_db;
