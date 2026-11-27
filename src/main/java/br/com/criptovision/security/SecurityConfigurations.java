package br.com.criptovision.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
    CorsProperties.class,
    TentativasProperties.class
})
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;
    private final AutenticacaoEntryPoint autenticacaoEntryPoint;
    private final CorsProperties corsProperties;

    public SecurityConfigurations(
        SecurityFilter securityFilter,
        AutenticacaoEntryPoint autenticacaoEntryPoint,
        CorsProperties corsProperties
    ) {
        this.securityFilter = securityFilter;
        this.autenticacaoEntryPoint = autenticacaoEntryPoint;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(
                corsConfigurationSource()
            ))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(
                    autenticacaoEntryPoint
                )
            )
            .authorizeHttpRequests(req -> {
                req.requestMatchers(
                    "/auth/login",
                    "/auth/register"
                ).permitAll();

                req.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll();

                req.anyRequest().authenticated();
            })
            .addFilterBefore(
                securityFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
            new CorsConfiguration();

        // origens vindas de CORS_ALLOWED_ORIGINS, já validadas por CorsProperties
        configuration.setAllowedOrigins(
            corsProperties.allowedOrigins()
        );

        configuration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}
