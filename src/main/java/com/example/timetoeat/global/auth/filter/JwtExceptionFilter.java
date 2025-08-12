package com.example.timetoeat.global.auth.filter;

import com.example.timetoeat.global.common.ApiResponse;
import com.example.timetoeat.global.error.BaseError;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            // JwtAuthenticatioonFilter에서 예외 발생시 바로 setErrorResponse 호출
            setErrorResponse(response, e);
        }
    }

    private void setErrorResponse(HttpServletResponse response, JwtException e) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized

        // ApiResponse 형식을 사용하여 에러 메시지를 JSON으로 생성
        ApiResponse<Object> errorResponse = ApiResponse.fail(
                GlobalErrorCode.UNAUTHORIZED
        );

        // JSON을 응답 Body에 직접 작성
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
