package com.example.timetoeat.global.auth.jwt;

import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.entity.MemberEntity;
import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.global.error.GlobalErrorCode;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.access-token-expiration}")
    private int accessTockenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private int refreshTockenExpiration;

    @PostConstruct
    public void init() {
        log.info("accessTokenExpiration = {}", accessTockenExpiration);
        log.info("refreshTokenExpiration = {}", refreshTockenExpiration);
    }

    private SecretKey secretKey;
    private JwtParser jwtParser;

    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .build();
    }

    public TokenDto issueToken(MemberEntity memberEntity, Date now) {
        String accessToken = generateAccessToken(memberEntity, now);
        String refreshToken = generateRefreshToken(memberEntity, now);

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenMaxAge(refreshTockenExpiration)
                .build();
    }

    public Date getRefreshTokenExpiration(Date now) {
        return new Date(now.getTime() + refreshTockenExpiration);
    }

    public String generateAccessToken(MemberEntity memberEntity, Date now) {
        return Jwts.builder()
                .subject(String.valueOf(memberEntity.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTockenExpiration))
                .claim("username", memberEntity.getUsername())
                .claim("email", memberEntity.getEmail())
                .claim("role", memberEntity.getRole())
                .signWith(secretKey)
                .compact();
    }

    String generateRefreshToken(MemberEntity memberEntity, Date now) {
        return Jwts.builder()
                .subject(String.valueOf(memberEntity.getId()))
                .issuedAt(now)
                .expiration(getRefreshTokenExpiration(now))
                .claim("typ", "refresh")
                .signWith(secretKey)
                .compact();
    }

    public void validate(String token) {
        try {
            this.getPayload(token);
        } catch (SecurityException e) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new CustomException(GlobalErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
    }

    public Claims getPayload(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getId(String token) {
        return Long.valueOf(getPayload(token).getSubject());
    }

    public String getUsername(String token) {
        return getPayload(token).get("username", String.class);
    }

    public String getEmail(String token) {
        return getPayload(token).get("email", String.class);
    }

    public String getRole(String token) {
        return getPayload(token).get("role", String.class);
    }
}
