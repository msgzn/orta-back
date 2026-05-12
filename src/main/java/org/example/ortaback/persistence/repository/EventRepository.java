package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.projection.EventClusterProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA-репозиторий для {@link Event}.
 *
 * <p>Геопространственные методы используют нативные SQL-запросы поверх PostGIS:
 * <ul>
 *   <li>{@code ST_DWithin(::geography)} — поиск в радиусе с метрической
 *       семантикой (cast к {@code geography} обязателен, иначе радиус
 *       трактуется в градусах SRID 4326);</li>
 *   <li>{@code ST_MakeEnvelope} + оператор {@code &&} — bbox-фильтр для
 *       видимой области карты, использует GiST-индекс на {@code location};</li>
 *   <li>KNN-оператор {@code <->} — сортировка по расстоянию (planar в SRID
 *       4326). Внутри города/региона погрешность пренебрежимо мала по сравнению
 *       с честным geodesic, но за это получаем index-only KNN-обход GiST;</li>
 *   <li>{@code ST_ClusterKMeans} — серверная агрегация маркеров при большом
 *       числе точек.</li>
 * </ul>
 *
 * <p>Векторный поиск ({@link #findSimilarEventsByEmbedding}) идёт через
 * pgvector cosine-distance {@code <=>}. Параметр {@code :embedding}
 * передаётся уже отформатированным в pgvector-текст
 * ({@code "[0.1,0.2,…]"}); сериализацию выполняет сервисный слой
 * (использует {@code persistence.type.VectorType} как образец).
 *
 * <p>Все «карточные» запросы отдают только {@link EventStatus#ACTIVE}-события —
 * отменённые и завершённые на карту не попадают. Для отдельных сценариев
 * (история, кабинет создателя) есть статус-агностические методы выше.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // -------------------------------------------------------------------------
    // Базовые derived-методы
    // -------------------------------------------------------------------------

    List<Event> findByCreatorId(Long creatorId);

    List<Event> findByCategoryId(Long categoryId);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /**
     * Лента событий: только {@code ACTIVE}, опциональный фильтр по категории.
     * Передавайте {@code null} в {@code categoryId}, чтобы вернуть все
     * категории. Сортировка задаётся через {@code Pageable}.
     */
    @Query("""
            SELECT e
              FROM Event e
             WHERE e.status = org.example.ortaback.persistence.models.enums.EventStatus.ACTIVE
               AND (:categoryId IS NULL OR e.category.id = :categoryId)
            """)
    Page<Event> findFeed(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * AI-рекомендации: ближайшие по cosine-distance к вектору интересов
     * пользователя, среди ACTIVE и без тех, в которые юзер уже вступил.
     *
     * <p>Параметр {@code embedding} принимается строкой в формате pgvector
     * ({@code "[0.1,0.2,…]"}) и кастуется на SQL-стороне.
     *
     * @param embedding     pgvector-текст
     * @param viewerId      исключаем события, в которых viewer уже JOINED
     * @param categoryId    {@code null} — без фильтра по категории
     * @param limit         сколько вернуть
     */
    @Query(value = """
            SELECT e.*
              FROM event e
             WHERE e.status = 'ACTIVE'
               AND e.event_embedding IS NOT NULL
               AND (:categoryId IS NULL OR e.category_id = :categoryId)
               AND NOT EXISTS (
                     SELECT 1 FROM event_participant ep
                      WHERE ep.event_id = e.id
                        AND ep.user_id  = :viewerId
                        AND ep.status   = 'JOINED'
                   )
             ORDER BY e.event_embedding <=> CAST(:embedding AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<Event> findRecommendedForUser(@Param("embedding") String embedding,
                                       @Param("viewerId") Long viewerId,
                                       @Param("categoryId") Long categoryId,
                                       @Param("limit") int limit);

    // -------------------------------------------------------------------------
    // Геопространственные запросы (PostGIS)
    //
    // NB о порядке координат: ST_MakePoint принимает (X, Y) = (lng, lat).
    //                         ST_MakeEnvelope принимает (xmin, ymin, xmax, ymax).
    // -------------------------------------------------------------------------

    /**
     * Поиск событий в радиусе {@code radiusMeters} метров от точки {@code (lat, lng)}.
     *
     * <p>Использует {@code ST_DWithin} с приведением к {@code geography} — это
     * гарантирует, что радиус интерпретируется в метрах (на плоскости geometry
     * SRID 4326 он был бы в градусах).
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
               AND ST_DWithin(
                       e.location::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                       :radiusMeters
                   )
            """, nativeQuery = true)
    List<Event> findEventsWithinRadius(@Param("lat") double lat,
                                       @Param("lng") double lng,
                                       @Param("radiusMeters") double radiusMeters);

    /**
     * Поиск событий в прямоугольной области карты — вызывается фронтендом при
     * перемещении/зуме карты. Оператор {@code &&} проверяет пересечение
     * bounding box'ов и эффективно использует GiST-индекс.
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
               AND e.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            """, nativeQuery = true)
    List<Event> findEventsInBoundingBox(@Param("minLat") double minLat,
                                        @Param("minLng") double minLng,
                                        @Param("maxLat") double maxLat,
                                        @Param("maxLng") double maxLng);

    /**
     * Поиск событий в радиусе с сортировкой по возрастанию расстояния от
     * указанной точки. Радиус-фильтр идёт по {@code ::geography} (метры),
     * сортировка — по planar-оператору {@code <->}, который умеет GiST KNN.
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
               AND ST_DWithin(
                       e.location::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                       :radiusMeters
                   )
             ORDER BY e.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            """, nativeQuery = true)
    List<Event> findEventsWithinRadiusOrderedByDistance(@Param("lat") double lat,
                                                        @Param("lng") double lng,
                                                        @Param("radiusMeters") double radiusMeters);

    /**
     * N ближайших к точке событий, независимо от расстояния. Используется,
     * например, на первом открытии карты, если у пользователя нет известной
     * геопозиции.
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
             ORDER BY e.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
             LIMIT :limit
            """, nativeQuery = true)
    List<Event> findNearestEvents(@Param("lat") double lat,
                                  @Param("lng") double lng,
                                  @Param("limit") int limit);

    /**
     * Комбинированный фильтр: категория + радиус. Также возвращает события
     * отсортированными по расстоянию.
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
               AND e.category_id = :categoryId
               AND ST_DWithin(
                       e.location::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                       :radiusMeters
                   )
             ORDER BY e.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            """, nativeQuery = true)
    List<Event> findEventsByCategoryWithinRadius(@Param("categoryId") Long categoryId,
                                                 @Param("lat") double lat,
                                                 @Param("lng") double lng,
                                                 @Param("radiusMeters") double radiusMeters);

    /**
     * Подсчёт событий в bbox — вызывается перед решением «кластеризовать
     * на сервере или отдать сырые маркеры». Порог сравнивается с
     * {@code application.geo.client-cluster-threshold}.
     */
    @Query(value = """
            SELECT COUNT(*) FROM event e
             WHERE e.status = 'ACTIVE'
               AND e.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            """, nativeQuery = true)
    long countEventsInBoundingBox(@Param("minLat") double minLat,
                                  @Param("minLng") double minLng,
                                  @Param("maxLat") double maxLat,
                                  @Param("maxLng") double maxLng);

    /**
     * Серверная кластеризация через {@code ST_ClusterKMeans}. Делит точки на
     * {@code numClusters} групп и возвращает центроид каждой плюс число
     * событий в кластере. Используется на малых масштабах карты.
     *
     * <p>Альтернатива на DBSCAN ({@code ST_ClusterDBSCAN}) делает кластеры
     * переменного размера по {@code eps} (метры) — её можно подключить
     * вторым запросом, переключение управляется
     * {@code application.geo.clustering.algorithm}.
     */
    @Query(value = """
            SELECT
                cluster_id                                  AS cluster_id,
                COUNT(*)                                    AS event_count,
                ST_X(ST_Centroid(ST_Collect(location)))     AS center_lng,
                ST_Y(ST_Centroid(ST_Collect(location)))     AS center_lat
            FROM (
                SELECT
                    ST_ClusterKMeans(location, :numClusters) OVER () AS cluster_id,
                    location
                FROM event
                WHERE status = 'ACTIVE'
                  AND location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            ) clustered
            GROUP BY cluster_id
            """, nativeQuery = true)
    List<EventClusterProjection> clusterEventsInBoundingBox(@Param("minLat") double minLat,
                                                            @Param("minLng") double minLng,
                                                            @Param("maxLat") double maxLat,
                                                            @Param("maxLng") double maxLng,
                                                            @Param("numClusters") int numClusters);

    // -------------------------------------------------------------------------
    // Векторный поиск (pgvector)
    // -------------------------------------------------------------------------

    /**
     * Top-{@code limit} событий, наиболее похожих по эмбеддингу
     * на заданный вектор. Использует cosine-distance {@code <=>}.
     *
     * @param embedding pgvector-текстовое представление, например
     *                  {@code "[0.12,-0.34,0.56,...]"}
     * @param excludeId id события, которое надо исключить из результатов
     *                  (обычно — само исходное событие). Передавайте
     *                  {@code -1L}, если исключать нечего.
     * @param limit     максимум возвращаемых записей.
     */
    @Query(value = """
            SELECT e.* FROM event e
             WHERE e.status = 'ACTIVE'
               AND e.event_embedding IS NOT NULL
               AND e.id <> :excludeId
             ORDER BY e.event_embedding <=> CAST(:embedding AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<Event> findSimilarEventsByEmbedding(@Param("embedding") String embedding,
                                             @Param("excludeId") Long excludeId,
                                             @Param("limit") int limit);
}
