package org.example.ortaback.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * База для интеграционных тестов, которым нужен ЖИВОЙ Spring-контекст поверх
 * реального PostgreSQL с PostGIS + pgvector (в отличие от slice-/standalone-
 * MockMvc-тестов, которые контекст не поднимают).
 *
 * <p><b>Образ.</b> Берём {@code orta/postgres:17-postgis-pgvector} — тот же, что
 * в {@code docker-compose.yml} (собирается из {@code Dockerfile.postgres}:
 * postgis/postgis 17-3.5 + pgvector 0.7.4 из исходников). Именно проектный
 * образ, а не public, — чтобы версии расширений в тестах совпадали с dev/prod.
 * <b>Если образа нет локально</b> ({@code docker images} его не показывает) —
 * собрать один раз: {@code docker compose build postgres}
 * (или {@code docker compose up -d --build}). Testcontainers сам образ НЕ
 * собирает — только запускает готовый.
 *
 * <p><b>Расширения.</b> {@code vector} обязателен уже на этапе DDL: сущности
 * объявляют {@code columnDefinition = "vector(384)"}
 * ({@code Event.event_embedding}, {@code User.global_interest_embedding}).
 * Создаём их через {@code db/test-init.sql} ({@code withInitScript}), который
 * прогоняется по JDBC сразу после старта контейнера и ДО Hibernate ddl-auto.
 *
 * <p><b>Жизненный цикл.</b> Singleton-pattern: контейнер стартует один раз в
 * статическом блоке и живёт до конца JVM (его гасит Testcontainers Ryuk).
 * Намеренно НЕ используем {@code @Testcontainers}/{@code @Container} — они
 * перезапускали бы контейнер на каждый тест-класс; нам же выгодно переиспользовать
 * один инстанс между всеми IT.
 *
 * <p>Datasource перебивается на контейнер через {@code @DynamicPropertySource};
 * остальной {@code application.yml} наследуется как есть (ddl-auto:update,
 * PostgreSQLDialect + hibernate-spatial, а Redis/S3/mail спроектированы
 * fail-open и контекст без них поднимается).
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("orta/postgres:17-postgis-pgvector")
            .asCompatibleSubstituteFor("postgres");

    @SuppressWarnings("resource") // singleton: контейнер закрывает Ryuk при выходе JVM
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("orta_test")
            .withUsername("orta")
            .withPassword("orta")
            .withInitScript("db/test-init.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
