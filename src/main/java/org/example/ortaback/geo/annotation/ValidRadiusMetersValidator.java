package org.example.ortaback.geo.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.ortaback.config.GeoProperties;

/**
 * Получает свой верхний предел из {@link GeoProperties} — Spring внедряет
 * бин через {@code SpringConstraintValidatorFactory}, который автоматически
 * подключается Spring Boot Starter Validation.
 */
public class ValidRadiusMetersValidator implements ConstraintValidator<ValidRadiusMeters, Double> {

    private final GeoProperties geoProperties;

    public ValidRadiusMetersValidator(GeoProperties geoProperties) {
        this.geoProperties = geoProperties;
    }

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value <= 0.0) {
            return setMessage(context, "Search radius must be > 0 meters");
        }
        double maxMeters = geoProperties.maxSearchRadiusKm() * 1000.0;
        if (value > maxMeters) {
            return setMessage(context,
                    "Search radius must not exceed " + (int) maxMeters + " meters");
        }
        return true;
    }

    private boolean setMessage(ConstraintValidatorContext ctx, String msg) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
