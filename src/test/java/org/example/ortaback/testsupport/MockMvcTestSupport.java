package org.example.ortaback.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.ortaback.exception.GlobalExceptionHandler;
import org.example.ortaback.persistence.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Базовая обвязка для контроллерных тестов в стиле standalone-MockMvc.
 *
 * <p>Подключает:
 * <ul>
 *   <li>{@link GlobalExceptionHandler} — реальный маппинг исключений → HTTP-статусы;</li>
 *   <li>{@link AuthenticationPrincipalArgumentResolver} — резолвер для
 *       {@code @AuthenticationPrincipal User};</li>
 *   <li>Jackson с {@link JavaTimeModule} (иначе {@code LocalDateTime} сериализуется
 *       как массив, а не ISO-строка);</li>
 *   <li>{@link LocalValidatorFactoryBean} — для {@code @Valid @RequestBody}.</li>
 * </ul>
 *
 * <p>NB: <em>не</em> подключает Spring Security фильтр и
 * {@code MethodValidationPostProcessor} — поэтому в этих тестах не
 * проверяется ни «401 без токена», ни валидация {@code @RequestParam} через
 * {@code @ValidLatitude}-стиль аннотации. Первое тестируется на уровне
 * SecurityConfig интеграционными тестами (отдельная итерация), второе —
 * на уровне {@code CoordinateValidator} unit-тестами.
 */
public abstract class MockMvcTestSupport {

    protected ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected MockMvc buildMockMvc(Object controller) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    /** Кладёт пользователя в {@code SecurityContext} как principal. */
    protected void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @BeforeEach
    void clearSecurityContextBefore() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContextAfter() {
        SecurityContextHolder.clearContext();
    }
}
