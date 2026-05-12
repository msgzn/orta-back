package org.example.ortaback.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceCalculatorTest {

    @Test
    void haversine_samePoint_returnsZero() {
        double m = DistanceCalculator.haversineMeters(43.2220, 76.8512, 43.2220, 76.8512);
        assertThat(m).isEqualTo(0.0);
    }

    @Test
    void haversine_oneDegreeOfLatitudeAtEquator_isAbout111Km() {
        double m = DistanceCalculator.haversineMeters(0.0, 0.0, 1.0, 0.0);
        // Один градус широты ≈ 111.2 км. Допуск 1 км на разницу сферической
        // модели vs WGS-84 эллипсоида.
        assertThat(m).isCloseTo(111_200.0, within(1_000.0));
    }

    @Test
    void haversine_almatyToAstana_isAbout970Km() {
        // Алматы (43.2220, 76.8512) → Астана (51.1605, 71.4704)
        double m = DistanceCalculator.haversineMeters(43.2220, 76.8512, 51.1605, 71.4704);
        assertThat(m).isCloseTo(970_000.0, within(10_000.0));
    }

    @Test
    void haversine_isSymmetric() {
        double a = DistanceCalculator.haversineMeters(43.2220, 76.8512, 51.1605, 71.4704);
        double b = DistanceCalculator.haversineMeters(51.1605, 71.4704, 43.2220, 76.8512);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void haversine_withPointOverload_matchesScalarVersion() {
        Point a = GeoUtils.point(43.2220, 76.8512);
        Point b = GeoUtils.point(51.1605, 71.4704);

        double scalar = DistanceCalculator.haversineMeters(43.2220, 76.8512, 51.1605, 71.4704);
        double points = DistanceCalculator.haversineMeters(a, b);

        assertThat(points).isEqualTo(scalar);
    }
}
