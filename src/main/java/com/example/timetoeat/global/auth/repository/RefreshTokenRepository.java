package com.example.timetoeat.global.auth.repository;

import com.example.timetoeat.global.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteAllByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
    boolean existsByToken(String refreshToken);
    Optional<RefreshToken> findByMemberId(Long memberId); // findByMemberId 추가
}
