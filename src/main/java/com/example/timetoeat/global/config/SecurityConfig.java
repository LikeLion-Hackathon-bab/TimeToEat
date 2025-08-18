package com.example.timetoeat.global.config;

import com.example.timetoeat.global.auth.filter.JwtExceptionFilter;
import com.example.timetoeat.global.auth.filter.JwtVerificationFilter;
import com.example.timetoeat.global.auth.handler.JwtAccessDeniedHandler;
import com.example.timetoeat.global.auth.handler.LoginSuccessHandler;
import com.example.timetoeat.global.auth.handler.OAuth2LoginFailureHandler;
import com.example.timetoeat.global.auth.handler.Oauth2EntryPoint;
import com.example.timetoeat.global.auth.service.CustomOauthUserService;
import com.example.timetoeat.global.auth.util.CookieOAuth2AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final CustomOauthUserService customOAuth2UserService;
    private final LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final Oauth2EntryPoint oAuth2EntryPoint;
    private final LogoutSuccessHandler jwtLogoutSuccessHandler;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtVerificationFilter jwtVerificationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository; // <-- 주입받도록 추가


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorizationEndpointConfig ->
                                        authorizationEndpointConfig
                                                .baseUri("/oauth2/authorization")
                                                .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                        )
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig
                                        .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                );

        http
                .logout(logout -> logout
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(jwtLogoutSuccessHandler)
                );

        // 커스텀 필터 등록 (순서 중요!)
        http
                .addFilterBefore(jwtVerificationFilter,  UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtVerificationFilter.class);

        http
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(oAuth2EntryPoint)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                );
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/",
                                "/api/v1/articles/*/ai/**",
                                "/api/v1/ai/support/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
