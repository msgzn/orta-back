-- =============================================================================
-- Расширения PostgreSQL для интеграционных тестов (Testcontainers).
-- =============================================================================
-- Выполняется по JDBC через PostgreSQLContainer.withInitScript(...) сразу после
-- старта контейнера и ДО того, как Hibernate (ddl-auto:update) начнёт создавать
-- схему. Это тестовый аналог боевого init.sql (тот монтируется в compose через
-- /docker-entrypoint-initdb.d/), но без psql-метакоманд (\c) и CREATE DATABASE —
-- здесь только то, что безопасно прогоняется одним JDBC-батчем.
--
-- Базовый образ postgis/postgis уже создаёт расширение postgis в тестовой БД,
-- но дублируем идемпотентно (IF NOT EXISTS) — дёшево и страхует от смены образа.
-- =============================================================================

-- PostGIS — geometry/geography-типы для Event.location, User.lastKnownLocation и т.п.
CREATE EXTENSION IF NOT EXISTS postgis;

-- pgvector — тип vector(N); НУЖЕН на этапе DDL, т.к. Event.event_embedding и
-- User.global_interest_embedding объявлены как columnDefinition = "vector(384)".
CREATE EXTENSION IF NOT EXISTS vector;

-- pg_trgm — нечёткий поиск (на DDL не влияет, но держим паритет с боевым init.sql).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
