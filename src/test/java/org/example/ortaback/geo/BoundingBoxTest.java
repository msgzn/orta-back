package org.example.ortaback.geo;

import org.example.ortaback.exception.InvalidCoordinatesException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundingBoxTest {

    @Test
    void of_acceptsValidBox() {
        BoundingBox b = BoundingBox.of(40.0, 70.0, 50.0, 80.0);
        assertThat(b.minLat()).isEqualTo(40.0);
        assertThat(b.maxLng()).isEqualTo(80.0);
    }

    @Test
    void of_rejectsLatitudeOutOfRange() {
        assertThatThrownBy(() -> BoundingBox.of(-91.0, 0.0, 50.0, 10.0))
                .isInstanceOf(InvalidCoordinatesException.class);
    }

    @Test
    void of_rejectsLongitudeOutOfRange() {
        assertThatThrownBy(() -> BoundingBox.of(40.0, -181.0, 50.0, 10.0))
                .isInstanceOf(InvalidCoordinatesException.class);
    }

    @Test
    void of_rejectsInvertedLatitudes() {
        assertThatThrownBy(() -> BoundingBox.of(60.0, 0.0, 50.0, 10.0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("minLat");
    }

    @Test
    void of_rejectsInvertedLongitudes() {
        assertThatThrownBy(() -> BoundingBox.of(40.0, 50.0, 50.0, 10.0))
                .isInstanceOf(InvalidCoordinatesException.class)
                .hasMessageContaining("minLng");
    }

    @Test
    void of_acceptsDegenerateBox() {
        // mins == maxs — нулевая площадь, но валидная пара корнеров.
        assertThatCode(() -> BoundingBox.of(45.0, 75.0, 45.0, 75.0))
                .doesNotThrowAnyException();
    }
}
