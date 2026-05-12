package org.example.ortaback.geo;

import org.example.ortaback.config.GeoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyMaskerTest {

    private PrivacyMasker masker;

    @BeforeEach
    void setUp() {
        GeoProperties props = new GeoProperties(
                4326, 10, 100, 1000, 50, 200,
                null,
                new GeoProperties.Privacy(500)  // 500-метровая сетка
        );
        masker = new PrivacyMasker(props);
    }

    @Test
    void snap_null_returnsNull() {
        assertThat(masker.snap(null)).isNull();
    }

    @Test
    void snap_displacesPointWithin500mForExactInput() {
        Point exact = GeoUtils.point(43.2220, 76.8512);
        Point snapped = masker.snap(exact);

        double moved = DistanceCalculator.haversineMeters(exact, snapped);
        // С шагом 500м снэп может сместить максимум на ~√2/2 * 500 ≈ 350м,
        // плюс небольшая дискретность. Даём с запасом 700м.
        assertThat(moved).isLessThan(700.0);
    }

    @Test
    void snap_secondSnapStaysWithinOneGridStep() {
        // Полная идемпотентность не выполняется: после первого снэпа широта
        // чуть меняется, а шаг по долготе зависит от широты через cos(lat) —
        // второй снэп может ещё чуть сдвинуть. Для приватности важно, чтобы
        // дополнительное смещение было ограничено примерно одним шагом сетки.
        Point exact = GeoUtils.point(43.2220, 76.8512);
        Point once = masker.snap(exact);
        Point twice = masker.snap(once);

        double additional = DistanceCalculator.haversineMeters(once, twice);
        assertThat(additional).isLessThan(500.0);
    }

    @Test
    void snap_preservesSrid4326() {
        Point snapped = masker.snap(GeoUtils.point(43.2220, 76.8512));
        assertThat(snapped.getSRID()).isEqualTo(4326);
    }

    @Test
    void snap_atHighLatitude_doesNotExplode() {
        // 89° — около полюса; cos(lat) близок к нулю, но защита 1e-6 не даст
        // делению взорваться. Просто проверяем, что не падает и SRID на месте.
        Point exact = GeoUtils.point(89.0, 0.0);
        Point snapped = masker.snap(exact);

        assertThat(snapped).isNotNull();
        assertThat(snapped.getSRID()).isEqualTo(4326);
    }

    @Test
    void snap_withCustomGrid_appliesProvidedStep() {
        Point exact = GeoUtils.point(43.2220, 76.8512);
        Point coarse = masker.snap(exact, 10_000);   // 10 км
        Point fine   = masker.snap(exact, 100);      // 100 м

        double coarseDisplacement = DistanceCalculator.haversineMeters(exact, coarse);
        double fineDisplacement   = DistanceCalculator.haversineMeters(exact, fine);

        // С более грубой сеткой смещение в среднем больше; на одной точке это
        // не строго так, но шкала разная — fine < 200м (один шаг), coarse
        // может быть до ~7 км.
        assertThat(fineDisplacement).isLessThan(200.0);
        assertThat(coarseDisplacement).isLessThan(15_000.0);
    }
}
