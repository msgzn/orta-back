package org.example.ortaback.geo;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;

class GeoUtilsTest {

    @Test
    void point_createsPointWithFlippedXYAndSrid4326() {
        Point p = GeoUtils.point(43.2220, 76.8512);

        assertThat(p.getY()).isEqualTo(43.2220);   // latitude
        assertThat(p.getX()).isEqualTo(76.8512);   // longitude
        assertThat(p.getSRID()).isEqualTo(4326);
    }

    @Test
    void latitudeAndLongitudeGetters_extractCorrectAxes() {
        Point p = GeoUtils.point(51.1605, 71.4704);

        assertThat(GeoUtils.latitude(p)).isEqualTo(51.1605);
        assertThat(GeoUtils.longitude(p)).isEqualTo(71.4704);
    }

    @Test
    void factory_isSingletonPinnedToSrid4326() {
        assertThat(GeoUtils.factory().getSRID()).isEqualTo(4326);
        assertThat(GeoUtils.factory()).isSameAs(GeoUtils.factory());
    }
}
