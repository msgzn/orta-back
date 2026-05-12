package org.example.ortaback.geo.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Проверяет, что число лежит в диапазоне географической долготы
 * {@code [-180, 180]}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidLongitudeValidator.class)
public @interface ValidLongitude {

    String message() default "Longitude must be in [-180, 180]";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
