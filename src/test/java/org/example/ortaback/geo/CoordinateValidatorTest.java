package org.example.ortaback.geo;

import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.exception.InvalidCoordinatesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordinateValidatorTest {

    private CoordinateValidator validator;

    @BeforeEach
    void setUp() {
        GeoProperties props = new GeoProperties(
                4326,           // srid
                10,             // defaultSearchRadiusKm
                100,            // maxSearchRadiusKm
                1000,           // clientClusterThreshold
                50, 200,        // nearest defaults
                null,           // clustering (не нужен этому валидатору)
                null            // privacy (тоже не нужен)
        );
        validator = new CoordinateValidator(props);
    }

    @Test
    void validateLatLng_acceptsCenter() {
        assertThatCode(() -> validator.validateLatLng(0.0, 0.0)).doesNotThrowAnyException();
    }

    @Test
    void validateLatLng_acceptsBoundaries() {
        assertThatCode(() -> validator.validateLatLng(90.0, 180.0)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateLatLng(-90.0, -180.0)).doesNotThrowAnyException();
    }

    @Test
    void validateLatLng_rejectsLatitudeAbove90() {
        assertThatThrownBy(() -> validator.validateLatLng(91.0, 0.0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("Latitude");
    }

    @Test
    void validateLatLng_rejectsLongitudeBelowMinus180() {
        assertThatThrownBy(() -> validator.validateLatLng(0.0, -181.0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("Longitude");
    }

    @Test
    void validateRadiusMeters_acceptsPositiveBelowCap() {
        assertThatCode(() -> validator.validateRadiusMeters(50_000)).doesNotThrowAnyException();
    }

    @Test
    void validateRadiusMeters_rejectsZero() {
        assertThatThrownBy(() -> validator.validateRadiusMeters(0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("> 0");
    }

    @Test
    void validateRadiusMeters_rejectsAboveMaxKmCap() {
        // max = 100 km = 100_000 m
        assertThatThrownBy(() -> validator.validateRadiusMeters(100_001))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    void validateBoundingBox_rejectsInvertedLatitudes() {
        assertThatThrownBy(() -> validator.validateBoundingBox(50.0, 0.0, 40.0, 10.0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("minLat");
    }

    @Test
    void validateBoundingBox_acceptsValidBox() {
        assertThatCode(() -> validator.validateBoundingBox(40.0, 0.0, 50.0, 10.0))
                .doesNotThrowAnyException();
    }

    @Test
    void constants_matchGeographicLimits() {
        assertThat(CoordinateValidator.LAT_MIN).isEqualTo(-90.0);
        assertThat(CoordinateValidator.LAT_MAX).isEqualTo(90.0);
        assertThat(CoordinateValidator.LNG_MIN).isEqualTo(-180.0);
        assertThat(CoordinateValidator.LNG_MAX).isEqualTo(180.0);
    }
}
