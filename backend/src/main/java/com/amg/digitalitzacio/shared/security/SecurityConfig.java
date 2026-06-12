package com.amg.digitalitzacio.shared.security;

import com.amg.digitalitzacio.shared.audit.AuditFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuditFilter auditFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // JWT stateless, no cal CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(Customizer.withDefaults())
                        .xssProtection(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/billing/budgets/preview").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/billing/budgets/accept",
                                "/api/v1/billing/budgets/accept-phases",
                                "/api/v1/billing/budgets/reject"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/engine/render/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/engine/render/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sitemap.xml").permitAll()
                        .requestMatchers(HttpMethod.POST, "/contact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assets/*/file").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assets/*/thumbnail").permitAll()
                        .requestMatchers("/api/v1/automations/webhook/**").permitAll()
                        .requestMatchers("/api/v1/finops/webhook/**").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/gocardless/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/gocardless/tenants/*/mandate/complete").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/telegram/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/telegram/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/whatsapp/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/whatsapp-meta/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/whatsapp-meta/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/email/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/email/inbound").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/nexe/calendar/oauth-callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/google/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/oauth/meta/whatsapp/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/meta/whatsapp/oauth-session/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/whatsapp/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/whatsapp/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chat/sessions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chat/sessions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/chat/agency/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chat/agency/sessions").permitAll()
                        .requestMatchers("/api/v1/widget/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ops/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/demo/inbox/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/demo/inbox/**").permitAll()
                        .requestMatchers("/api/v1/demo/**").permitAll()
                        // Swagger / OpenAPI (protegit — requereix autenticació)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/booking/*/days").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/booking/*/slots").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/booking/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/booking/*/confirm").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/api/v1/tenants/**").authenticated()
                        .requestMatchers("/api/v1/vault/**").authenticated()
                        .requestMatchers("/api/v1/engine/**").authenticated()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticat"))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Widget endpoints: cross-origin des de webs de clients (qualsevol domini)
        var widgetConfig = new CorsConfiguration();
        widgetConfig.setAllowedOriginPatterns(List.of("*"));
        widgetConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        widgetConfig.setAllowedHeaders(List.of("Content-Type", "X-Forwarded-For"));
        widgetConfig.setAllowCredentials(false);
        widgetConfig.setMaxAge(3600L);

        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/widget/**", widgetConfig);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
