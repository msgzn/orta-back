-- =============================================================================
-- ORTA backend — database initialization script
-- =============================================================================
-- Запускается ОДНОКРАТНО суперпользователем PostgreSQL до старта приложения.
-- DDL самих таблиц создаёт Hibernate / Flyway; здесь ставим только расширения,
-- которые невозможно добавить из JPA.
--
-- Использование (локально):
--   psql -U postgres -d orta_db -f init.sql
--
-- В Docker / docker-compose: положить этот файл в
--   /docker-entrypoint-initdb.d/init.sql
-- образа postgis/postgis:16-3.4 — он выполнится при первом старте контейнера.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Создание БД (выполнять при необходимости от имени суперпользователя)
-- ---------------------------------------------------------------------------
-- CREATE DATABASE orta_db
--     WITH OWNER = postgres
--          ENCODING = 'UTF8'
--          LC_COLLATE = 'en_US.UTF-8'
--          LC_CTYPE   = 'en_US.UTF-8'
--          TEMPLATE = template0;

-- \c orta_db

-- ---------------------------------------------------------------------------
-- 2. Обязательные расширения
-- ---------------------------------------------------------------------------

-- PostGIS — геопространственные типы (geometry, geography), функции
-- (ST_DWithin, ST_MakeEnvelope, ST_ClusterKMeans, ST_SnapToGrid, ...)
-- и GiST-индексы для пространственного поиска.
CREATE EXTENSION IF NOT EXISTS postgis;

-- pgvector — тип vector(N) и операторы расстояния:
--   <->  L2-distance      (евклидово)
--   <#>  inner-product
--   <=>  cosine-distance  (используем для AI-матчинга по эмбеддингам)
CREATE EXTENSION IF NOT EXISTS vector;

-- (опционально) pg_trgm — нечёткий поиск по названиям событий / адресам.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- 3. Проверка установки
-- ---------------------------------------------------------------------------
SELECT extname, extversion
FROM   pg_extension
WHERE  extname IN ('postgis', 'vector', 'pg_trgm');

-- =============================================================================
-- NB: GiST-индексы на geometry-поля и IVFFlat/HNSW-индексы на vector-поля
-- будут созданы в DDL-миграциях (Flyway) или через @Index в JPA-сущностях.
-- Пример того, что появится после слоя Entities:
--
--   CREATE INDEX idx_event_location           ON event           USING GIST (location);
--   CREATE INDEX idx_user_last_known_location ON app_user        USING GIST (last_known_location);
--   CREATE INDEX idx_university_campus        ON university      USING GIST (campus_location);
--   CREATE INDEX idx_event_embedding_cosine   ON event           USING HNSW (event_embedding vector_cosine_ops);
--   CREATE INDEX idx_user_interest_embedding  ON app_user        USING HNSW (global_interest_embedding vector_cosine_ops);
-- =============================================================================
