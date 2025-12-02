package com.cipherbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Allow these origins
        config.setAllowedOrigins(Arrays.asList(
                "http://cipher.thepaytrix.com",
                "http://43.205.156.91",
                "http://43.205.156.91:8081",
                "http://localhost:3000"  // For local development
        ));

        // Allow all headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Allow these HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        // Apply CORS to all endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}