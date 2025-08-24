package com.example.timetoeat.domain.member.repository;

import com.example.timetoeat.domain.member.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.memberId = :memberId")
    Optional<RefreshToken> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    Optional<RefreshToken> findByMemberId(Long memberId);

    boolean existsByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
