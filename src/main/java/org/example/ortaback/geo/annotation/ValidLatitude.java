package org.example.ortaback.geo.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Проверяет, что число лежит в диапазоне географической широты
 * {@code [-90, 90]}. Применима к {@code Double} / {@code double}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidLatitudeValidator.class)
public @interface ValidLatitude {

    String message() default "Latitude must be in [-90, 90]";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
