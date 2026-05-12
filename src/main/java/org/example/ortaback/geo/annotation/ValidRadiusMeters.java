package org.example.ortaback.geo.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Валидирует радиус поиска (в метрах) против глобального максимума,
 * заданного в {@code application.geo.max-search-radius-km}.
 *
 * <p>Радиус должен быть строго положительным.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRadiusMetersValidator.class)
public @interface ValidRadiusMeters {

    String message() default "Search radius is out of allowed range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
