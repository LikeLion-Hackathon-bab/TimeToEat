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

        // [수정] try-catch 블록을 제거하여 예외가 발생하면 JwtExceptionFilter가 처리하도록 합니다.
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 토큰 유효성 검증. 실패 시 JwtException이 발생하여 JwtExceptionFilter로 넘어감.
            jwtProvider.validate(token);

            // 토큰이 유효하면 인증 정보를 생성하여 SecurityContext에 저장
            Long memberId = jwtProvider.getId(token);
            String email = jwtProvider.getEmail(token);
            String role = jwtProvider.getRole(token);
            String username = jwtProvider.getUsername(token);

            Oauth2UserInfo userInfo = Oauth2UserInfo.fromJwt(memberId, username, email, Role.valueOf(role));
            CustomOauth2User customOauth2User = new CustomOauth2User(userInfo);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customOauth2User,
                    null,
                    customOauth2User.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/");
    }
}
