package com.example.timetoeat.global.auth.filter;

import com.example.timetoeat.global.auth.jwt.JwtProvider;
import com.example.timetoeat.global.auth.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String bearerToken = request.getHeader("Authorization");
        String accessToken = jwtService.getTokenFromBearer(bearerToken);

        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response); // 토큰 없으면 인증 없이 통과
            return;
        }

        jwtProvider.validate(accessToken);
        //::todo:: 일단 저장해주는건 넘기고
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/");
    }
}
