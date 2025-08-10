package com.example.timetoeat.global.auth.filter;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.jwt.JwtProvider;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            try {
                jwtProvider.validate(token);
                Long memberId = jwtProvider.getId(token);
                String email = jwtProvider.getEmail(token);
                String role = jwtProvider.getRole(token);
                String username = jwtProvider.getUsername(token);

                Oauth2UserInfo userInfo = Oauth2UserInfo.fromJwt(memberId, username, email, Role.valueOf(role));

                CustomOauth2User customOauth2User = new CustomOauth2User(userInfo);

                // 5. Authentication 객체 생성 및 SecurityContextHolder에 저장
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        customOauth2User, // Principal (인증된 사용자 정보)
                        null,             // Credentials (비밀번호 등, 보통 null로 처리)
                        customOauth2User.getAuthorities() // Authorities (권한 목록)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);


            } catch (Exception e) {
                log.warn("JWT 인증 실패: {}", e.getMessage());
            }
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/");
    }
}
