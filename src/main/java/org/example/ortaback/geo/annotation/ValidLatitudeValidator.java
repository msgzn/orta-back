package org.example.ortaback.geo.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidLatitudeValidator implements ConstraintValidator<ValidLatitude, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        // null оставляем валидным — для опциональности используется @NotNull отдельно.
        return value == null || (value >= -90.0 && value <= 90.0);
    }
}
