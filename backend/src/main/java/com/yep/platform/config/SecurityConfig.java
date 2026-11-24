package com.yep.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yep.platform.common.ApiError;
import com.yep.platform.security.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Delegating encoder: bcrypt by default, upgradeable without breaking existing hashes. */
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt, RateLimitFilter rate, ObjectMapper mapper) throws Exception {
        http
            .cors(c -> {})
            // Tokens are in cookies, so CSRF protection stays ON (double-submit cookie). The SPA echoes XSRF-TOKEN in X-XSRF-TOKEN.
            // Only unauthenticated, session-less endpoints are exempt (nothing to ride on).
            .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/certificates/verify/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/*", "/api/exchange-sessions", "/api/projects", "/api/mentors", "/api/public/stats").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(mapper, res, req.getRequestURI(), 401, "Unauthorized", "Please log in to continue."))
                .accessDeniedHandler((req, res, ex) -> write(mapper, res, req.getRequestURI(), 403, "Forbidden", "You do not have permission to do this.")))
            .headers(h -> h.contentTypeOptions(c -> {}).frameOptions(f -> f.deny())
                    .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                    .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .addFilterBefore(rate, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    private static void write(ObjectMapper m, jakarta.servlet.http.HttpServletResponse res, String path, int status, String error, String msg) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        m.writeValue(res.getOutputStream(), ApiError.of(status, error, msg, path));
    }

    /** These filters belong to the security chain only; stop Boot also registering them as global servlet filters. */
    @Bean FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter f) { var b = new FilterRegistrationBean<>(f); b.setEnabled(false); return b; }
    @Bean FilterRegistrationBean<RateLimitFilter> rateFilterRegistration(RateLimitFilter f) { var b = new FilterRegistrationBean<>(f); b.setEnabled(false); return b; }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${yep.cors.allowed-origins}") List<String> origins) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", c);
        return src;
    }
}
