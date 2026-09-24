package com.smartparking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.OAuth2LoginSuccessHandler;
import com.smartparking.service.CustomOAuth2UserService;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/auth/**")
                .permitAll()
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(
                    (request, response, authException) -> {
                        response.setStatus(
                            HttpStatus.UNAUTHORIZED.value()
                        );
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"message\":\"Unauthorized\"}"
                        );
                    })
                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                        response.setStatus(
                            HttpStatus.FORBIDDEN.value()
                        );
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"message\":\"Forbidden\"}"
                        );
                    })
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> {})

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/health",
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/error"
                ).permitAll()

                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")

                .anyRequest()
                .authenticated()
            )

            .exceptionHandling(exception -> exception

                .authenticationEntryPoint(
                    (request, response, authException) -> {

                        response.setStatus(
                                HttpStatus.UNAUTHORIZED.value()
                        );

                        response.setContentType(
                                "application/json"
                        );

                        response.getWriter().write(
                                "{\"message\":\"Unauthorized\"}"
                        );
                    })

                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {

                        response.setStatus(
                                HttpStatus.FORBIDDEN.value()
                        );

                        response.setContentType(
                                "application/json"
                        );

                        response.getWriter().write(
                                "{\"message\":\"Forbidden\"}"
                        );
                    })
            )

            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(
                            customOAuth2UserService
                    )
                )
                .successHandler(
                        oauth2LoginSuccessHandler
                )
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}