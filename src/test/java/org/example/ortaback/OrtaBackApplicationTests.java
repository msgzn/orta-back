package org.example.ortaback;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke-test on full Spring context. Disabled by default — поднимает реальный
 * контекст, который требует живую БД с PostGIS + pgvector. Включить после
 * подключения Testcontainers-итерации (postgis-pgvector образ + Hibernate
 * spatial dialect автоопределение).
 */
@SpringBootTest
@Disabled("Requires real PostgreSQL with PostGIS + pgvector — enable when Testcontainers setup is in place")
class OrtaBackApplicationTests {

    @Test
    void contextLoads() {
    }
}
