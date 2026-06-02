package org.example.ortaback;

import org.example.ortaback.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke-тест полного Spring-контекста поверх РЕАЛЬНОГО PostgreSQL
 * (PostGIS + pgvector) через Testcontainers — см. {@link AbstractIntegrationTest}.
 *
 * <p>Проверяет, что контекст поднимается целиком и Hibernate (ddl-auto:update)
 * успешно создаёт схему, включая geometry- и {@code vector(384)}-колонки,
 * а ApplicationRunner-сидеры (категории, вузы) отрабатывают без ошибок.
 */
class OrtaBackApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
