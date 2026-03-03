package org.project.appointment_project.common.config;

import lombok.RequiredArgsConstructor;
import org.project.appointment_project.common.security.jwt.converter.CustomJwtAuthenticationConverter;
import org.project.appointment_project.common.security.jwt.handler.JwtAccessDeniedHandler;
import org.project.appointment_project.common.security.jwt.handler.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String[] GET_PUBLIC = {
            "/api/doctors/**",
            "/api/schedules/**",
            "/api/specialties/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // LUỒNG 1: Ưu tiên xử lý API trước
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // Chỉ bắt các URL bắt đầu bằng /api/
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, GET_PUBLIC).permitAll()
                        .requestMatchers("/api/payments/vnpay/callback").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/register/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(customJwtAuthenticationConverter))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    // LUỒNG 2: Lưới hứng (Fallback) cho toàn bộ ZK UI và Static Resources
    @Bean
    @Order(2)
    public SecurityFilterChain zkFilterChain(HttpSecurity http) throws Exception {
        http
                // BỎ HẲN .securityMatcher() ở đây.
                // Nó sẽ tự động bắt MỌI request KHÔNG PHẢI là /api/ (VD: /index.zul, /user/doctor.zul, /zkau...)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Tạm thời mở toàn bộ UI để test luồng
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf
                        // Chỉ cần tắt CSRF cho đường dẫn AJAX ngầm của ZK (Path này hợp lệ 100% với Spring Boot 3)
                        .ignoringRequestMatchers("/zkau/**")
                );

        return http.build();
    }
}