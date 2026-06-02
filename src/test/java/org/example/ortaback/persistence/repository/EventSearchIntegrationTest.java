package org.example.ortaback.persistence.repository;

import org.example.ortaback.geo.GeoUtils;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест нативного {@link EventRepository#search} поверх РЕАЛЬНОГО
 * PostGIS (Testcontainers — см. {@link AbstractIntegrationTest}). Нативный SQL
 * НЕ валидируется при старте контекста — только в рантайме при вызове метода,
 * поэтому каждую ветку {@code CAST(:p AS …) IS NULL} нужно прогнать против живой
 * БД, иначе опечатка в SQL не всплыла бы в standalone-тестах.
 *
 * <p>Имя класса оканчивается на {@code Test} (а не {@code IT}) намеренно: в pom
 * не подключён failsafe-плагин, и интеграционные тесты гоняются обычным
 * surefire в фазе {@code test} (та же конвенция, что у
 * {@code OrtaBackApplicationTests}). Суффикс {@code IT} surefire по умолчанию
 * НЕ подхватывает — тест молча не запустился бы.
 *
 * <p>{@code @Transactional} → каждый метод откатывается; {@code saveAndFlush}
 * гарантирует, что строки уже в БД (но не закоммичены) к моменту нативного
 * запроса в той же транзакции/соединении.
 */
@Transactional
class EventSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    /** Алматы, район КазНУ — центр гео-проверок. */
    private static final double LAT = 43.238;
    private static final double LNG = 76.945;

    private Category category;
    private User organizer;

    @BeforeEach
    void seed() {
        category = categoryRepository.saveAndFlush(
                Category.builder().name("Football-" + System.nanoTime()).build());

        organizer = userRepository.saveAndFlush(User.builder()
                .firstName("Org").lastName("Anizer")
                .email("org-" + System.nanoTime() + "@example.com")
                .hashedPassword("BCRYPT")
                .trustScore(40.0f)              // Надёжный (≥30)
                .build());
    }

    private Event persistEvent(String title, BigDecimal price, double lat, double lng) {
        return eventRepository.saveAndFlush(Event.builder()
                .creator(organizer)
                .category(category)
                .status(EventStatus.ACTIVE)
                .title(title)
                .description("desc")
                .location(GeoUtils.point(lat, lng))
                .locationPrecision(LocationPrecision.EXACT)
                .startTime(LocalDateTime.of(2026, 7, 1, 18, 0))
                .maxParticipants(10)
                .pricePerPerson(price)
                .build());
    }

    private static PageRequest firstPage() {
        return PageRequest.of(0, 20);   // без Sort — ORDER BY зашит в нативном SQL
    }

    /**
     * Все фильтры заданы одновременно — главное доказательство, что нативный
     * SQL валиден на каждой ветке (text + category + date + geo + free + group +
     * trust выполняются разом).
     */
    @Test
    void search_allFiltersTogether_executesAndMatches() {
        persistEvent("Combined Football", null, LAT, LNG);

        Page<Event> result = eventRepository.search(
                "football", category.getId(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59),
                LAT, LNG, 5000.0,
                Boolean.TRUE, 1, 100, 30f,
                firstPage());

        assertThat(result.getContent()).extracting(Event::getTitle)
                .containsExactly("Combined Football");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void search_byText_isCaseInsensitiveOverTitle() {
        persistEvent("Evening Football at KazNU", null, LAT, LNG);
        persistEvent("Morning Yoga", null, LAT, LNG);

        Page<Event> hit = eventRepository.search(
                "football", null, null, null, null, null, null,
                null, null, null, null, firstPage());
        assertThat(hit.getContent()).extracting(Event::getTitle)
                .containsExactly("Evening Football at KazNU");

        Page<Event> miss = eventRepository.search(
                "swimming", null, null, null, null, null, null,
                null, null, null, null, firstPage());
        assertThat(miss.getContent()).isEmpty();
    }

    @Test
    void search_free_separatesPaidFromFree() {
        persistEvent("Free Run", null, LAT, LNG);
        persistEvent("Paid Tennis", new BigDecimal("2000.00"), LAT, LNG);

        Page<Event> free = eventRepository.search(
                null, null, null, null, null, null, null,
                Boolean.TRUE, null, null, null, firstPage());
        assertThat(free.getContent()).extracting(Event::getTitle).containsExactly("Free Run");

        Page<Event> paid = eventRepository.search(
                null, null, null, null, null, null, null,
                Boolean.FALSE, null, null, null, firstPage());
        assertThat(paid.getContent()).extracting(Event::getTitle).containsExactly("Paid Tennis");
    }

    @Test
    void search_organizerMinTrust_filtersByCreatorTrustScore() {
        persistEvent("Trusted Org Event", null, LAT, LNG);   // organizer trust = 40

        Page<Event> passes = eventRepository.search(
                null, null, null, null, null, null, null,
                null, null, null, 30f, firstPage());
        assertThat(passes.getContent()).hasSize(1);

        Page<Event> filtered = eventRepository.search(
                null, null, null, null, null, null, null,
                null, null, null, 60f, firstPage());   // выше, чем 40
        assertThat(filtered.getContent()).isEmpty();
    }

    @Test
    void search_geoRadius_filtersByDistanceInMeters() {
        persistEvent("Nearby", null, LAT, LNG);
        persistEvent("Far Away", null, LAT + 0.5, LNG + 0.5);   // ~60+ км

        Page<Event> near = eventRepository.search(
                null, null, null, null, LAT, LNG, 2000.0,
                null, null, null, null, firstPage());
        assertThat(near.getContent()).extracting(Event::getTitle).containsExactly("Nearby");
    }
}
