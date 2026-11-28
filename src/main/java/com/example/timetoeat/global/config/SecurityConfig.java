package com.example.timetoeat.global.config;

import com.example.timetoeat.global.auth.filter.JwtExceptionFilter;
import com.example.timetoeat.global.auth.filter.JwtVerificationFilter;
import com.example.timetoeat.global.auth.handler.JwtAccessDeniedHandler;
import com.example.timetoeat.global.auth.handler.LoginSuccessHandler;
import com.example.timetoeat.global.auth.handler.OAuth2LoginFailureHandler;
import com.example.timetoeat.global.auth.handler.Oauth2EntryPoint;
import com.example.timetoeat.global.auth.service.CustomOauthUserService;
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
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
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
                                                .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository())
                                                // 인가 요청(state)은 Spring 기본값인 HTTP 세션에 둔다. 서버 한 대(EC2 1대) 구성이라
                                                // 쿠키 저장소의 이점(다중 인스턴스)이 없고, 쿠키 저장소는 Java 역직렬화 표면만 늘린다.
                                                // 근거: qa-cloud/spring-followups/DECISIONS.md
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
                        // 로그인 전에 부르는 경로와 서버 간 호출(각자 헤더 키로 보호)만 연다. 나머지는 모두 인증 필요.
                        // 이전에는 "/api/v1/**" 전체가 열려 있어 /plans, /members/{id}/profile/detail 등이 토큰 없이 읽혔다.
                        .requestMatchers(
                                "/login/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/",
                                "/error",
                                "/api/v1/auth/refresh",                   // 리프레시 쿠키로 인증
                                "/api/v1/auth/logout",                    // 만료된 access 토큰으로도 로그아웃
                                "/api/v1/ai/support/**"                   // X-AI-KEY (AI 서버)
                        ).permitAll()
                        // Socket → Spring 결과 저장(X-Room-Key). POST만 연다. 같은 경로의 GET 상세는 인증 필요.
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/meetings/*/result",
                                "/api/v1/meetings/invitations/*/result"
                        ).permitAll()
                        .anyRequest().authenticated()
                );


        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(allowedOrigins(System.getenv("CORS_ALLOWED_ORIGINS")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "X-Requested-With",
                "accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Authorization"
        ));
        configuration.addExposedHeader("X-Access-Token");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static final List<String> DEFAULT_ORIGINS = List.of(
            "https://bab-muk-dang-client.vercel.app");
    // 만료 도메인(babmuckdang.site)·반납된 해커톤 IP·localhost는 자격증명 CORS 기본값에 두지 않는다(보안 점검 M2).
    // 운영 주소와 로컬 개발 출처(예 http://localhost:3001)는 CORS_ALLOWED_ORIGINS로 넣는다.

    /** 기본 목록 + CORS_ALLOWED_ORIGINS(쉼표 구분, 끝 슬래시 제거). 와일드카드는 받지 않는다(allowCredentials=true). */
    static List<String> allowedOrigins(String extra) {
        var origins = new java.util.LinkedHashSet<>(DEFAULT_ORIGINS);
        if (extra != null) {
            for (String o : extra.split(",")) {
                String v = o.trim().replaceFirst("/+$", "");
                if (!v.isEmpty() && !v.contains("*")) origins.add(v);
            }
        }
        return List.copyOf(origins);
    }
}
