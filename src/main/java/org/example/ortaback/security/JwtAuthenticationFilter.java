package org.example.ortaback.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр читает {@code Authorization: Bearer <token>}, валидирует подпись и
 * срок действия токена, убеждается, что это именно <em>access</em>-токен
 * (refresh здесь нелегитимен), грузит {@code UserDetails} по {@code sub}
 * и кладёт {@code Authentication} в {@code SecurityContextHolder}.
 *
 * <p>NB: не помечен {@code @Component} специально — регистрируется как
 * {@code @Bean} внутри {@code SecurityConfig}, иначе Spring Boot мог бы
 * добавить его и в глобальную servlet-цепочку фильтров, и он стрелял бы дважды.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null
                && tokenProvider.isValid(token)
                && tokenProvider.isAccessToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails user = userDetailsService.loadUserByUsername(
                        tokenProvider.getUsername(token));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (UsernameNotFoundException ex) {
                // Юзера в БД уже нет (удалили/деактивировали) — пропускаем
                // как анонимного, чтобы entryPoint вернул чистый 401.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
